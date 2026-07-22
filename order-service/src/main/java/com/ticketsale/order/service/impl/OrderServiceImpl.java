package com.ticketsale.order.service.impl;

import com.ticketsale.order.client.InventoryClient;
import com.ticketsale.order.controller.dto.request.CreateOrderRequest;
import com.ticketsale.order.controller.dto.response.OrderResponse;
import com.ticketsale.order.repository.OrderRepository;
import com.ticketsale.order.repository.entity.OrderEntity;
import com.ticketsale.order.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private static final long PAYMENT_TIMEOUT_MINUTES = 15;

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            InventoryClient inventoryClient
    ) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
    }

    // Tạo order chờ thanh toán. Phải giữ vé trước, rồi mới lưu order.
    @Override
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
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

        return toResponse(saved);
    }

    // Tìm order bằng mã public, không bắt frontend sử dụng ID database.
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByOrderNo(String orderNo) {
        OrderEntity entity = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy order")
                );

        return toResponse(entity);
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