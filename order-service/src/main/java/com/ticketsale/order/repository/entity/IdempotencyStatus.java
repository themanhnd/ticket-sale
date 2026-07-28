package com.ticketsale.order.repository.entity;

/**
 * Trạng thái xử lý của một Idempotency-Key.
 */
public enum IdempotencyStatus {

    // Request đã được nhận và đang chạy nghiệp vụ tạo order.
    PROCESSING,

    // Request đã xử lý xong và đã lưu response.
    COMPLETED
}