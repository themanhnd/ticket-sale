package com.ticketsale.order.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketsale.order.client.InventoryClient;
import com.ticketsale.order.controller.dto.request.CreateOrderRequest;
import com.ticketsale.order.controller.dto.response.CheckoutResponse;
import com.ticketsale.order.controller.dto.response.OrderResponse;
import com.ticketsale.order.exception.IdempotencyConflictException;
import com.ticketsale.order.repository.IdempotencyRecordRepository;
import com.ticketsale.order.repository.OrderRepository;
import com.ticketsale.order.repository.entity.IdempotencyRecordEntity;
import com.ticketsale.order.repository.entity.IdempotencyStatus;
import com.ticketsale.order.repository.entity.OrderEntity;
import com.ticketsale.order.service.OrderService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private static final long PAYMENT_TIMEOUT_MINUTES = 15;
    private static final long IDEMPOTENCY_TTL_HOURS = 24;
    private static final String ORDER_CREATE_SCOPE = "ORDER_CREATE";

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            InventoryClient inventoryClient,
            IdempotencyRecordRepository idempotencyRecordRepository,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
    }

    // Tạo order chờ thanh toán, chống request lặp bằng Idempotency-Key trước khi giữ vé.
    @Override
    @Transactional
    public OrderResponse create(CreateOrderRequest request, String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String ownerId = request.userId().toString();
        String requestHash = hashRequest(request);
        LocalDateTime now = LocalDateTime.now();

        IdempotencyRecordEntity activeRecord = idempotencyRecordRepository
                .findByScopeAndOwnerIdAndIdempotencyKey(ORDER_CREATE_SCOPE, ownerId, normalizedKey)
                .filter(record -> keepActiveRecord(record, now))
                .orElse(null);

        if (activeRecord != null) {
            return handleExistingRecord(activeRecord, requestHash);
        }

        IdempotencyRecordEntity newRecord = createProcessingRecord(ownerId, normalizedKey, requestHash, now);
        reserveIdempotencyKey(newRecord);

        inventoryClient.reserve(request.eventId(), request.quantity());

        String orderNo = "ORD-" + UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusMinutes(PAYMENT_TIMEOUT_MINUTES);

        OrderEntity entity = new OrderEntity(
                orderNo,
                request.userId(),
                request.eventId(),
                request.quantity(),
                expiresAt
        );

        OrderEntity saved = orderRepository.save(entity);
        OrderResponse response = toResponse(saved);
        newRecord.complete(writeResponse(response));

        return response;
    }

    // Tìm order bằng mã public, không bắt frontend sử dụng ID database.
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByOrderNo(String orderNo) {
        return toResponse(findByOrderNo(orderNo));
    }

    // Trả dữ liệu checkout gọn nhẹ để frontend thăm dò trạng thái thanh toán.
    @Override
    @Transactional(readOnly = true)
    public CheckoutResponse getCheckout(String orderNo) {
        OrderEntity entity = findByOrderNo(orderNo);

        return new CheckoutResponse(
                entity.getOrderNo(),
                entity.getStatus(),
                entity.getExpiresAt()
        );
    }

    // Chuẩn hóa key để key chỉ gồm khoảng trắng không lọt xuống tầng DB.
    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key không được để trống");
        }

        return idempotencyKey.trim();
    }

    // Giữ record còn hạn; xóa record hết hạn để client có thể dùng lại key sau TTL.
    private boolean keepActiveRecord(IdempotencyRecordEntity record, LocalDateTime now) {
        if (!record.isExpired(now)) {
            return true;
        }

        idempotencyRecordRepository.delete(record);
        idempotencyRecordRepository.flush();

        return false;
    }

    // Xử lý key đã tồn tại: trả response cũ nếu giống request, hoặc báo conflict nếu khác request.
    private OrderResponse handleExistingRecord(IdempotencyRecordEntity record, String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException("Idempotency-Key đã được dùng cho request khác");
        }

        if (record.getStatus() == IdempotencyStatus.PROCESSING) {
            throw new IdempotencyConflictException("Request với Idempotency-Key này đang được xử lý");
        }

        return readResponse(record.getResponseBody());
    }

    // Tạo record PROCESSING trước side effect để request trùng không giữ vé lần hai.
    private IdempotencyRecordEntity createProcessingRecord(
            String ownerId,
            String idempotencyKey,
            String requestHash,
            LocalDateTime now
    ) {
        return new IdempotencyRecordEntity(
                ORDER_CREATE_SCOPE,
                ownerId,
                idempotencyKey,
                requestHash,
                now.plusHours(IDEMPOTENCY_TTL_HOURS)
        );
    }

    // Ghi record ngay để unique key trong MySQL khóa thao tác double-click đồng thời.
    private void reserveIdempotencyKey(IdempotencyRecordEntity record) {
        try {
            idempotencyRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException exception) {
            throw new IdempotencyConflictException("Request với Idempotency-Key này đang được xử lý");
        }
    }

    // Băm phần nghiệp vụ của request để phát hiện cùng key nhưng body bị đổi.
    private String hashRequest(CreateOrderRequest request) {
        String rawRequest = request.userId() + "|" + request.eventId() + "|" + request.quantity();

        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(rawRequest.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Không tạo được request hash", exception);
        }
    }

    // Serialize response để lần gọi lặp không phải tạo order mới.
    private String writeResponse(OrderResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không lưu được idempotency response", exception);
        }
    }

    // Deserialize response cũ đã lưu trong idempotency record.
    private OrderResponse readResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Idempotency record thiếu response");
        }

        try {
            return objectMapper.readValue(responseBody, OrderResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không đọc được idempotency response", exception);
        }
    }

    // Gom logic tìm order để các API dùng cùng một lỗi khi order không tồn tại.
    private OrderEntity findByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy order")
                );
    }

    // Chuyển Entity nội bộ thành DTO trả ra ngoài.
    private OrderResponse toResponse(OrderEntity entity) {
        return new OrderResponse(
                entity.getId(),
                entity.getOrderNo(),
                entity.getUserId(),
                entity.getEventId(),
                entity.getQuantity(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
