package com.ticketsale.common.security;

public record AuthenticatedUser(
        Long userId,
        String email,
        String role
) {
}