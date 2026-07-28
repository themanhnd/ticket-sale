package com.ticketsale.order.repository.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Đại diện cho một record trong bảng idempotency_records.
 */
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecordEntity {

    // ID tự tăng do MySQL tạo.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nghiệp vụ sử dụng key, ví dụ ORDER_CREATE.
    @Column(nullable = false, length = 50)
    private String scope;

    // Người sở hữu key, hiện tại là userId.
    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;

    // Giá trị header Idempotency-Key do client gửi.
    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    // Dấu vân tay của request body để phát hiện cùng key nhưng body khác.
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    // Trạng thái xử lý key: PROCESSING hoặc COMPLETED.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IdempotencyStatus status;

    // Response JSON đã trả cho client sau khi tạo order thành công.
    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    // Thời điểm bắt đầu xử lý request.
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Thời điểm key hết hiệu lực.
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * JPA cần constructor rỗng để tạo object từ dữ liệu DB.
     */
    protected IdempotencyRecordEntity() {
    }

    /**
     * Tạo record mới khi backend bắt đầu xử lý một Idempotency-Key.
     */
    public IdempotencyRecordEntity(String scope, String ownerId, String idempotencyKey, String requestHash, LocalDateTime expiresAt) {
        this.scope = scope;
        this.ownerId = ownerId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.status = IdempotencyStatus.PROCESSING;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    /**
     * Đánh dấu request đã xử lý xong và lưu response để lần gọi lặp trả lại dữ liệu cũ.
     */
    public void complete(String responseBody) {
        this.responseBody = responseBody;
        this.status = IdempotencyStatus.COMPLETED;
    }

    /**
     * Kiểm tra key đã hết hạn hay chưa.
     */
    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }

}