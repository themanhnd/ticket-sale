package com.ticketsale.inventory.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateInventoryRequest(
        @NotNull(message = "Event id không được để trống")
        Long eventId,

        @NotNull(message = "Tổng số vé không được để trống")
        @Min(value = 1, message = "Tổng số vé phải lớn hơn 0")
        Integer totalQuantity
) {
}