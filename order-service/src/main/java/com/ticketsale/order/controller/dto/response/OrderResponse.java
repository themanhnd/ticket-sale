package com.ticketsale.order.controller.dto.response;

import com.ticketsale.order.repository.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        String orderNo,
        Long userId,
        Long eventId,
        Integer quantity,
        OrderStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}