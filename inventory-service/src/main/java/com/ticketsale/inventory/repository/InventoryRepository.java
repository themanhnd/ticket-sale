package com.ticketsale.inventory.repository;

import com.ticketsale.inventory.repository.entity.InventoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository
        extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findByEventId(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE) // Khóa dòng inventory của event để transaction khác phải chờ, tránh bán quá số vé.
    @Query("""
            select inventory
            from InventoryEntity inventory
            where inventory.eventId = :eventId
            """) // JPQL tìm inventory theo eventId, Hibernate sinh SQL tương ứng với event_id.
    Optional<InventoryEntity> findByEventIdForUpdate(
            @Param("eventId") Long eventId
    );
}