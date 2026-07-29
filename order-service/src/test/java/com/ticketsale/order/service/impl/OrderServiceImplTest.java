package com.ticketsale.order.service.impl;

import com.ticketsale.order.client.InventoryClient;
import com.ticketsale.order.controller.dto.request.CreateOrderRequest;
import com.ticketsale.order.controller.dto.response.CheckoutResponse;
import com.ticketsale.order.controller.dto.response.OrderResponse;
import com.ticketsale.order.repository.IdempotencyRecordRepository;
import com.ticketsale.order.repository.OrderRepository;
import com.ticketsale.order.repository.entity.IdempotencyRecordEntity;
import com.ticketsale.order.repository.entity.IdempotencyStatus;
import com.ticketsale.order.repository.entity.OrderEntity;
import com.ticketsale.order.repository.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final OrderServiceImpl orderService = new OrderServiceImpl(
            orderRepository,
            inventoryClient,
            idempotencyRecordRepository,
            objectMapper
    );

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

        assertEquals("Request đang được xử lý", exception.getMessage());
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

    @Test
    void createShouldReturnStoredResponseWhenCompletedRequestIsRepeated() {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1001L, 2);

        String storedResponseBody = """
                {
                  "id": 10,
                  "orderNo": "ORD-OLD",
                  "userId": 1,
                  "eventId": 1001,
                  "quantity": 2,
                  "status": "PENDING_PAYMENT",
                  "expiresAt": "2026-07-29T10:15:00",
                  "createdAt": "2026-07-29T10:00:00",
                  "updatedAt": "2026-07-29T10:00:00"
                }
                """;

        IdempotencyRecordEntity existingRecord = new IdempotencyRecordEntity(
                "ORDER_CREATE",
                "1",
                "order-key-123",
                "4e9103edf20eb4b14609e2a79cc6c5cf8465c00690d05b176fde05c0e8fd70cc",
                LocalDateTime.now().plusHours(24)
        );
        existingRecord.complete(storedResponseBody);

        when(idempotencyRecordRepository.findByScopeAndOwnerIdAndIdempotencyKey(
                "ORDER_CREATE",
                "1",
                "order-key-123"
        )).thenReturn(Optional.of(existingRecord));

        OrderResponse response = assertDoesNotThrow(
                () -> orderService.create(request, "order-key-123")
        );

        assertEquals(10L, response.id());
        assertEquals("ORD-OLD", response.orderNo());
        verify(inventoryClient, never()).reserve(any(), any());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void createShouldCompleteIdempotencyRecordAfterOrderIsCreated() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1001L, 2);

        when(idempotencyRecordRepository.findByScopeAndOwnerIdAndIdempotencyKey(
                "ORDER_CREATE",
                "1",
                "order-key-123"
        )).thenReturn(Optional.empty());
        when(idempotencyRecordRepository.saveAndFlush(any(IdempotencyRecordEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.create(request, "order-key-123");

        ArgumentCaptor<IdempotencyRecordEntity> recordCaptor =
                ArgumentCaptor.forClass(IdempotencyRecordEntity.class);
        verify(idempotencyRecordRepository).saveAndFlush(recordCaptor.capture());

        IdempotencyRecordEntity record = recordCaptor.getValue();
        assertEquals(IdempotencyStatus.COMPLETED, record.getStatus());

        OrderResponse storedResponse = objectMapper.readValue(
                record.getResponseBody(),
                OrderResponse.class
        );
        assertEquals(response.orderNo(), storedResponse.orderNo());
        assertEquals(response.eventId(), storedResponse.eventId());
        assertEquals(response.quantity(), storedResponse.quantity());
    }

    @Test
    void createShouldDeleteExpiredIdempotencyRecordAndCreateNewOrder() {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1001L, 2);
        IdempotencyRecordEntity expiredRecord = new IdempotencyRecordEntity(
                "ORDER_CREATE",
                "1",
                "order-key-123",
                "different-hash",
                LocalDateTime.now().minusMinutes(1)
        );

        when(idempotencyRecordRepository.findByScopeAndOwnerIdAndIdempotencyKey(
                "ORDER_CREATE",
                "1",
                "order-key-123"
        )).thenReturn(Optional.of(expiredRecord));
        when(idempotencyRecordRepository.saveAndFlush(any(IdempotencyRecordEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.create(request, "order-key-123");

        InOrder callOrder = inOrder(idempotencyRecordRepository, inventoryClient, orderRepository);
        callOrder.verify(idempotencyRecordRepository).delete(expiredRecord);
        callOrder.verify(idempotencyRecordRepository).flush();
        callOrder.verify(idempotencyRecordRepository).saveAndFlush(any(IdempotencyRecordEntity.class));
        callOrder.verify(inventoryClient).reserve(1001L, 2);
        callOrder.verify(orderRepository).save(any(OrderEntity.class));

        assertNotNull(response.orderNo());
        assertEquals(1001L, response.eventId());
        assertEquals(2, response.quantity());
    }
}