package com.ticketsale.order.service.impl;

import com.ticketsale.order.client.InventoryClient;
import com.ticketsale.order.controller.dto.request.CreateOrderRequest;
import com.ticketsale.order.controller.dto.response.CheckoutResponse;
import com.ticketsale.order.controller.dto.response.OrderResponse;
import com.ticketsale.order.repository.IdempotencyRecordRepository;
import com.ticketsale.order.repository.OrderRepository;
import com.ticketsale.order.repository.entity.OrderEntity;
import com.ticketsale.order.repository.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import com.ticketsale.order.repository.entity.IdempotencyRecordEntity;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final InventoryClient inventoryClient = mock(InventoryClient.class);
    private final IdempotencyRecordRepository idempotencyRecordRepository = mock(IdempotencyRecordRepository.class);
    private final OrderServiceImpl orderService = new OrderServiceImpl(orderRepository, inventoryClient, idempotencyRecordRepository);

    @Test
    void createShouldReserveInventoryBeforeSavingOrder() {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1001L, 2);

        when(idempotencyRecordRepository.findByScopeAndOwnerIdAndIdempotencyKey("ORDER_CREATE", "1", "order-key-123")).thenReturn(Optional.empty());

        when(idempotencyRecordRepository.saveAndFlush(any(IdempotencyRecordEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.create(request, "order-key-123");

        InOrder callOrder = inOrder(idempotencyRecordRepository, inventoryClient, orderRepository);

        callOrder.verify(idempotencyRecordRepository).findByScopeAndOwnerIdAndIdempotencyKey("ORDER_CREATE", "1", "order-key-123");

        callOrder.verify(idempotencyRecordRepository).saveAndFlush(any(IdempotencyRecordEntity.class));

        callOrder.verify(inventoryClient).reserve(1001L, 2);
        callOrder.verify(orderRepository).save(any(OrderEntity.class));

        assertNotNull(response.orderNo());
        assertEquals(1L, response.userId());
        assertEquals(1001L, response.eventId());
        assertEquals(2, response.quantity());
        assertEquals(OrderStatus.PENDING_PAYMENT, response.status());
    }

    @Test
    void createShouldNotSaveOrderWhenInventoryReserveFails() {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1001L, 99);
        doThrow(new IllegalArgumentException("Không đủ vé để giữ chỗ")).when(inventoryClient).reserve(1001L, 99);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> orderService.create(request, "order-key-456"));

        assertEquals("Không đủ vé để giữ chỗ", exception.getMessage());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void getCheckoutShouldReturnStatusAndExpiration() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        OrderEntity entity = new OrderEntity("ORD-123", 1L, 1001L, 2, expiresAt);
        when(orderRepository.findByOrderNo("ORD-123")).thenReturn(Optional.of(entity));

        CheckoutResponse response = orderService.getCheckout("ORD-123");

        assertEquals("ORD-123", response.orderNo());
        assertEquals(OrderStatus.PENDING_PAYMENT, response.status());
        assertEquals(expiresAt, response.expiresAt());
    }

    @Test
    void createShouldNotReserveInventoryWhenIdempotencyKeyExists() {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1001L, 2);
        IdempotencyRecordEntity existingRecord = new IdempotencyRecordEntity("ORDER_CREATE", "1", "order-key-123", "4e9103edf20eb4b14609e2a79cc6c5cf8465c00690d05b176fde05c0e8fd70cc", LocalDateTime.now().plusHours(24));

        when(idempotencyRecordRepository.findByScopeAndOwnerIdAndIdempotencyKey("ORDER_CREATE", "1", "order-key-123")).thenReturn(Optional.of(existingRecord));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> orderService.create(request, "order-key-123"));

        assertEquals("Idempotency-Key đã tồn tại", exception.getMessage());
        verify(inventoryClient, never()).reserve(any(), any());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void createShouldRejectSameIdempotencyKeyWithDifferentRequestBody() {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1001L, 2);
        IdempotencyRecordEntity existingRecord = new IdempotencyRecordEntity(
                "ORDER_CREATE",
                "1",
                "order-key-123",
                "different-hash",
                LocalDateTime.now().plusHours(24)
        );

        when(idempotencyRecordRepository.findByScopeAndOwnerIdAndIdempotencyKey(
                "ORDER_CREATE",
                "1",
                "order-key-123"
        )).thenReturn(Optional.of(existingRecord));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.create(request, "order-key-123")
        );

        assertEquals("Idempotency-Key đã được dùng cho request khác", exception.getMessage());
        verify(inventoryClient, never()).reserve(any(), any());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }
}