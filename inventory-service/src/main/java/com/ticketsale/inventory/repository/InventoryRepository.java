package com.ticketsale.inventory.repository;

import com.ticketsale.inventory.repository.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findByEventId(Long eventId);
}