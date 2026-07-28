package com.ticketsale.order.service.impl;

import com.ticketsale.order.client.InventoryClient;
import com.ticketsale.order.controller.dto.request.CreateOrderRequest;
import com.ticketsale.order.controller.dto.response.CheckoutResponse;
import com.ticketsale.order.controller.dto.response.OrderResponse;
import com.ticketsale.order.repository.OrderRepository;
import com.ticketsale.order.repository.entity.OrderEntity;
import com.ticketsale.order.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ticketsale.order.repository.IdempotencyRecordRepository;
import com.ticketsale.order.repository.entity.IdempotencyRecordEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private static final long PAYMENT_TIMEOUT_MINUTES = 15;

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private static final long IDEMPOTENCY_TTL_HOURS = 24;
    private static final String ORDER_CREATE_SCOPE = "ORDER_CREATE";

    public OrderServiceImpl(
            OrderRepository orderRepository,
            InventoryClient inventoryClient,
            IdempotencyRecordRepository idempotencyRecordRepository
    ) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    // Tạo order chờ thanh toán. Phải giữ vé trước, rồi mới lưu order.
    @Override
    @Transactional
    public OrderResponse create(CreateOrderRequest request, String idempotencyKey) {
        String ownerId = request.userId().toString();
        String requestHash = hashRequest(request);
        IdempotencyRecordEntity existingRecord = idempotencyRecordRepository
                .findByScopeAndOwnerIdAndIdempotencyKey(
                        ORDER_CREATE_SCOPE,
                        ownerId,
                        idempotencyKey.trim()
                )
                .orElse(null);

        if (existingRecord != null && !existingRecord.getRequestHash().equals(requestHash)) {
            throw new IllegalArgumentException("Idempotency-Key đã được dùng cho request khác");
        }

        if (existingRecord != null) {
            // ponytail: Tạm chặn request lặp cùng nội dung; bước sau sẽ trả response cũ.
            throw new IllegalArgumentException("Idempotency-Key đã tồn tại");
        }
        IdempotencyRecordEntity record = new IdempotencyRecordEntity(
                ORDER_CREATE_SCOPE,
                ownerId,
                idempotencyKey.trim(),
                requestHash,
                LocalDateTime.now().plusHours(IDEMPOTENCY_TTL_HOURS)
        );

        idempotencyRecordRepository.saveAndFlush(record);

        inventoryClient.reserve(request.eventId(), request.quantity());

        String orderNo = "ORD-" + UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PAYMENT_TIMEOUT_MINUTES);

        OrderEntity entity = new OrderEntity(orderNo, request.userId(), request.eventId(), request.quantity(), expiresAt);

        OrderEntity saved = orderRepository.save(entity);

        return toResponse(saved);
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

        return new CheckoutResponse(entity.getOrderNo(), entity.getStatus(), entity.getExpiresAt());
    }

    // Gom logic tìm order để các API dùng cùng một lỗi khi order không tồn tại.
    private OrderEntity findByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy order"));
    }

    // Chuyển Entity nội bộ thành DTO trả ra ngoài.
    private OrderResponse toResponse(OrderEntity entity) {
        return new OrderResponse(entity.getId(), entity.getOrderNo(), entity.getUserId(), entity.getEventId(), entity.getQuantity(), entity.getStatus(), entity.getExpiresAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    /**
     * Tạo dấu vân tay SHA-256 từ nội dung request.
     */
    private String hashRequest(CreateOrderRequest request) {
        String rawRequest =
                request.userId() + "|" +
                        request.eventId() + "|" +
                        request.quantity();

        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(rawRequest.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Không tạo được request hash",
                    exception
            );
        }
    }
}