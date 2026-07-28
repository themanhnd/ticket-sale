package com.ticketsale.order.repository;

import com.ticketsale.order.repository.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Truy vấn bảng idempotency_records.
 */
public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecordEntity, Long> {

    /**
     * Tìm record theo bộ khóa chống trùng request.
     */
    Optional<IdempotencyRecordEntity> findByScopeAndOwnerIdAndIdempotencyKey(
            String scope,
            String ownerId,
            String idempotencyKey
    );
}