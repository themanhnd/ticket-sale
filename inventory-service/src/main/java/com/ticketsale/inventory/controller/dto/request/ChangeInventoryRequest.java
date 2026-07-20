package com.ticketsale.inventory.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;


public record ChangeInventoryRequest(
        // Chỉ nhận quantity vì eventId lấy từ URL.
        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        Integer quantity
) {
}