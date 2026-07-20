package com.ticketsale.inventory.service.impl;

import com.ticketsale.inventory.controller.dto.response.InventoryResponse;
import com.ticketsale.inventory.repository.InventoryRepository;
import com.ticketsale.inventory.repository.entity.InventoryEntity;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryServiceImplTest {

    private final InventoryRepository inventoryRepository = mock(InventoryRepository.class);
    private final InventoryServiceImpl inventoryService = new InventoryServiceImpl(inventoryRepository);

    @Test
    void reserveShouldDecreaseAvailableQuantity() {
        InventoryEntity entity = new InventoryEntity(1L, 100);
        when(inventoryRepository.findByEventIdForUpdate(1L)).thenReturn(Optional.of(entity));

        InventoryResponse response = inventoryService.reserve(1L, 2);

        assertEquals(98, response.availableQuantity());
    }

    @Test
    void reserveShouldFailWhenAvailableQuantityIsNotEnough() {
        InventoryEntity entity = new InventoryEntity(1L, 2);
        when(inventoryRepository.findByEventIdForUpdate(1L)).thenReturn(Optional.of(entity));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.reserve(1L, 3)
        );

        assertEquals("Không đủ vé để giữ chỗ", exception.getMessage());
    }

    @Test
    void reserveShouldFailWhenQuantityIsNotPositive() {
        InventoryEntity entity = new InventoryEntity(1L, 100);
        when(inventoryRepository.findByEventIdForUpdate(1L)).thenReturn(Optional.of(entity));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.reserve(1L, 0)
        );

        assertEquals("Số lượng phải lớn hơn 0", exception.getMessage());
    }

    @Test
    void releaseShouldIncreaseAvailableQuantity() {
        InventoryEntity entity = new InventoryEntity(1L, 100);
        entity.reserve(2);
        when(inventoryRepository.findByEventIdForUpdate(1L)).thenReturn(Optional.of(entity));

        InventoryResponse response = inventoryService.release(1L, 1);

        assertEquals(99, response.availableQuantity());
    }

    @Test
    void releaseShouldFailWhenAvailableQuantityExceedsTotalQuantity() {
        InventoryEntity entity = new InventoryEntity(1L, 100);
        when(inventoryRepository.findByEventIdForUpdate(1L)).thenReturn(Optional.of(entity));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.release(1L, 1)
        );

        assertEquals("Số vé trả lại vượt quá tổng số vé", exception.getMessage());
    }
}