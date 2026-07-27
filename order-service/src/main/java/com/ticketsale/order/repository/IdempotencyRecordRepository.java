package com.ticketsale.order.repository;

import com.ticketsale.order.repository.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, Long> {

    Optional<IdempotencyRecordEntity> findByScopeAndOwnerIdAndIdempotencyKey(
            String scope,
            String ownerId,
            String idempotencyKey
    );
}
