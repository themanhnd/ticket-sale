package com.ticketsale.order.controller.dto.response;

import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        String orderNo,
        Long userId,
        Long eventId,
        Integer quantity,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}