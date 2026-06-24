package com.ticketsale.user.controller.dto.response;

import java.time.LocalDateTime;

// Dữ liệu trả về cho frontend, không trả thẳng Entity.
public record UserResponse(
        Long id,
        String email,
        String fullName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
