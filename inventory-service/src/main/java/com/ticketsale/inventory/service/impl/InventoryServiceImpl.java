package com.ticketsale.inventory.service.impl;

import com.ticketsale.inventory.controller.dto.request.CreateInventoryRequest;
import com.ticketsale.inventory.controller.dto.response.InventoryResponse;
import com.ticketsale.inventory.repository.InventoryRepository;
import com.ticketsale.inventory.repository.entity.InventoryEntity;
import com.ticketsale.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional
    public InventoryResponse create(CreateInventoryRequest request) {
        inventoryRepository.findByEventId(request.eventId()).ifPresent(existing -> {
            throw new IllegalArgumentException("Inventory của event đã tồn tại");
        });

        InventoryEntity saved = inventoryRepository.save(new InventoryEntity(
                request.eventId(),
                request.totalQuantity()
        ));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getByEventId(Long eventId) {
        InventoryEntity entity = inventoryRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy inventory của event"));
        return toResponse(entity);
    }

    @Override
    @Transactional // Transaction giữ khóa pessimistic đến khi reserve xong.
    public InventoryResponse reserve(Long eventId, Integer quantity) {
        InventoryEntity entity = inventoryRepository
                .findByEventIdForUpdate(eventId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy inventory của event"
                        )
                );

        entity.reserve(quantity);

        return toResponse(entity);
    }

    @Override
    @Transactional
    public InventoryResponse release(Long eventId, Integer quantity) {
        InventoryEntity entity = inventoryRepository
                .findByEventIdForUpdate(eventId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy inventory của event"
                        )
                );

        entity.release(quantity);

        return toResponse(entity);
    }

    private InventoryResponse toResponse(InventoryEntity entity) {
        return new InventoryResponse(
                entity.getId(),
                entity.getEventId(),
                entity.getTotalQuantity(),
                entity.getAvailableQuantity(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}