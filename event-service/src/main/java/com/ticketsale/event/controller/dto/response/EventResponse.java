package com.ticketsale.event.controller.dto.response;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String code,
        String name,
        String location,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}