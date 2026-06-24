package com.ticketsale.common.idempotency;
//Đây là idempotency cho API hay consumer
public record IdempotencyKey(
        IdempotencyScope scope,
        String ownerId, // Ai sở hữu key này , ví dụ : user id , service name , consumer group logic
        String key // Giá trị thực tế : request id , idempotency-key header , event id
) {

    public IdempotencyKey {
        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId is required");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key is required");
        }
    }
}
