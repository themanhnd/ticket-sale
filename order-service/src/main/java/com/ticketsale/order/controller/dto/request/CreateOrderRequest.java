package com.ticketsale.order.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull(message = "User id không được để trống")
        Long userId,

        @NotNull(message = "Event id không được để trống")
        Long eventId,

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        Integer quantity
) {
}