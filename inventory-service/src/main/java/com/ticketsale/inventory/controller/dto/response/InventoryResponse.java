package com.ticketsale.inventory.controller.dto.response;

import java.time.LocalDateTime;

public record InventoryResponse(
        Long id,
        Long eventId,
        Integer totalQuantity,
        Integer availableQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}