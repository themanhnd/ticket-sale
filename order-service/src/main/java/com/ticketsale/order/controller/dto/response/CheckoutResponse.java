package com.ticketsale.order.controller.dto.response;

import com.ticketsale.order.repository.entity.OrderStatus;

import java.time.LocalDateTime;

// Dữ liệu tối thiểu để frontend theo dõi trạng thái thanh toán của order.
public record CheckoutResponse(
        String orderNo,
        OrderStatus status,
        LocalDateTime expiresAt
) {
}
