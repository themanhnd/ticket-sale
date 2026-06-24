package com.ticketsale.template.controller.dto.response;

import java.time.LocalDateTime;

// Dữ liệu trả về cho frontend, không trả thẳng Entity.
public record TemplateResponse(
        Long id,
        String code,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}