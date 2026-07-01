package com.ticketsale.event.repository;

import com.ticketsale.event.repository.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    Optional<EventEntity> findByCode(String code);
}