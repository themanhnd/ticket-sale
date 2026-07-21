package com.ticketsale.order.service.impl;

import com.ticketsale.order.controller.dto.request.CreateOrderRequest;
import com.ticketsale.order.controller.dto.response.OrderResponse;
import com.ticketsale.order.repository.OrderRepository;
import com.ticketsale.order.repository.entity.OrderEntity;
import com.ticketsale.order.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Xử lý nghiệp vụ chính của template.
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository OrderRepository;

    public OrderServiceImpl(OrderRepository OrderRepository) {
        this.OrderRepository = OrderRepository;
    }

    @Override
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        OrderRepository.findByCode(request.code()).ifPresent(existing -> {
            throw new IllegalArgumentException("Mã đã tồn tại");
        });

        OrderEntity saved = OrderRepository.save(new OrderEntity(request.code(), request.name()));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        OrderEntity entity = OrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy template"));
        return toResponse(entity);
    }

    // Chuyển Entity nội bộ thành DTO trả ra ngoài.
    private OrderResponse toResponse(OrderEntity entity) {
        return new OrderResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}