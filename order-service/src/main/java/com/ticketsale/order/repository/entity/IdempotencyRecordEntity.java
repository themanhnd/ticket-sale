package com.ticketsale.order.repository.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_idempotency_records_scope_owner_key",
                columnNames = {"scope", "owner_id", "idempotency_key"}
        )
)
public class IdempotencyRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String scope;

    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IdempotencyStatus status;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected IdempotencyRecordEntity() {
    }

    public IdempotencyRecordEntity(
            String scope,
            String ownerId,
            String idempotencyKey,
            String requestHash,
            LocalDateTime expiresAt
    ) {
        this.scope = scope;
        this.ownerId = ownerId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.status = IdempotencyStatus.PROCESSING;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    // Đánh dấu request đã xử lý xong và lưu response để lần gọi lặp trả lại đúng dữ liệu cũ.
    public void complete(String responseBody) {
        this.responseBody = responseBody;
        this.status = IdempotencyStatus.COMPLETED;
    }

    // Kiểm tra TTL để key cũ sau 24 giờ có thể được dùng lại như thao tác mới.
    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public String getScope() {
        return scope;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
