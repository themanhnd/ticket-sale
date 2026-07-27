package com.ticketsale.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticketsale.order.client.InventoryClient;
import com.ticketsale.order.controller.dto.request.CreateOrderRequest;
import com.ticketsale.order.controller.dto.response.CheckoutResponse;
import com.ticketsale.order.controller.dto.response.OrderResponse;
import com.ticketsale.order.exception.IdempotencyConflictException;
import com.ticketsale.order.repository.IdempotencyRecordRepository;
import com.ticketsale.order.repository.OrderRepository;
import com.ticketsale.order.repository.entity.IdempotencyRecordEntity;
import com.ticketsale.order.repository.entity.IdempotencyStatus;
import com.ticketsale.order.repository.entity.OrderEntity;
import com.ticketsale.order.repository.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    private static final String ORDER_CREATE_SCOPE = "ORDER_CREATE";

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final InventoryClient inventoryClient = mock(InventoryClient.class);
    private final IdempotencyRecordRepository idempotencyRecordRepository = mock(IdempotencyRecordRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final OrderServiceImpl orderService = new OrderServiceImpl(
            orderRepository,
            inventoryClient,
            idempotencyRecordRepository,
            objectMapper
    );

    @Test
    void createShouldReserveInventoryBeforeSavingOrder() {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1001L, 2);
        when(idempotencyRecordRepository.findByScopeAndOwnerIdAndIdempotencyKey(
                ORDER_CREATE_SCOPE,
                "1",
                "order-key-123"
        )).thenReturn(Optional.empty());
        when(idempotencyRecordRepository.saveAndFlush(any(IdempotencyRecordEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.create(request, "order-key-123");

        InOrder callOrder = inOrder(idempotencyRecordRepository, inventoryClient, orderRepository);
        callOrder.verify(idempotencyRecordRepository).saveAndFlush(any(IdempotencyRecordEntity.class));
        callOrder.verify(inventoryClient).reserve(1001L, 2);
        callOrder.verify(orderRepository).save(any(OrderEntity.class));

        ArgumentCaptor<IdempotencyRecordEntity> recordCaptor = ArgumentCaptor.forClass(IdempotencyRecordEntity.class);
        verify(idempotencyRecordRepository).saveAndFlush(recordCaptor.capture());
        assertEquals(IdempotencyStatus.COMPLETED, recordCaptor.getValue().getStatus());
        assertNotNull(recordCaptor.getValue().getResponseBody());

        assertNotNull(response.orderNo());
        assertEquals(1L, response.userId());
        assertEquals(1001L, response.eventId());
        assertEquals(2, response.quantity());
        assertEquals(OrderStatus.PENDING_PAYMENT, response.status());
    }

    @Test
    void createShouldNotSaveOrderWhenInventoryReserveFails() {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1001L, 99);
        when(idempotencyRecordRepository.findByScopeAndOwnerIdAndIdempotencyKey(
                ORDER_CREATE_SCOPE,
                "1",
                "order-key-456"
        )).thenReturn(Optional.empty());
        when(idempotencyRecordRepository.saveAndFlush(any(IdempotencyRecordEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalArgumentException("Không đủ vé để giữ chỗ"))
                .when(inventoryClient)
                .reserve(1001L, 99);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.create(request, "order-key-456")
        );

        assertEquals("Không đủ vé để giữ chỗ", exception.getMessage());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void createShouldReturnStoredResponseWhenSameKeyAndSameBody() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1001L, 2);
        LocalDateTime now = LocalDateTime.now();
        OrderResponse storedResponse = new OrderResponse(
                10L,
                "ORD-OLD",
                1L,
                1001L,
                2,
                OrderStatus.PENDING_PAYMENT,
                now.plusMinutes(15),
                now,
                now
        );
        IdempotencyRecordEntity record = completedRecord(request, "order-key-123", storedResponse);

        when(idempotencyRecordRepository.findByScopeAndOwnerIdAndIdempotencyKey(
                ORDER_CREATE_SCOPE,
                "1",
                "order-key-123"
        )).thenReturn(Optional.of(record));

        OrderResponse response = orderService.create(request, "order-key-123");

        assertEquals("ORD-OLD", response.orderNo());
        assertEquals(10L, response.id());
        verifyNoInteractions(inventoryClient);
        verify(orderRepository, never()).save(any(OrderEntity.class));
        verify(idempotencyRecordRepository, never()).saveAndFlush(any(IdempotencyRecordEntity.class));
    }

    @Test
    void createShouldRejectSameKeyWithDifferentBody() throws Exception {
        CreateOrderRequest firstRequest = new CreateOrderRequest(1L, 1001L, 2);
        CreateOrderRequest secondRequest = new CreateOrderRequest(1L, 1001L, 3);
        LocalDateTime now = LocalDateTime.now();
        OrderResponse storedResponse = new OrderResponse(
                10L,
                "ORD-OLD",
                1L,
                1001L,
                2,
                OrderStatus.PENDING_PAYMENT,
                now.plusMinutes(15),
                now,
                now
        );
        IdempotencyRecordEntity record = completedRecord(firstRequest, "order-key-123", storedResponse);

        when(idempotencyRecordRepository.findByScopeAndOwnerIdAndIdempotencyKey(
                ORDER_CREATE_SCOPE,
                "1",
                "order-key-123"
        )).thenReturn(Optional.of(record));

        IdempotencyConflictException exception = assertThrows(
                IdempotencyConflictException.class,
                () -> orderService.create(secondRequest, "order-key-123")
        );

        assertEquals("Idempotency-Key đã được dùng cho request khác", exception.getMessage());
        verifyNoInteractions(inventoryClient);
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void createShouldRejectSameKeyWhileFirstRequestIsProcessing() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1001L, 2);
        IdempotencyRecordEntity record = new IdempotencyRecordEntity(
                ORDER_CREATE_SCOPE,
                "1",
                "order-key-123",
                requestHash(request),
                LocalDateTime.now().plusHours(24)
        );

        when(idempotencyRecordRepository.findByScopeAndOwnerIdAndIdempotencyKey(
                ORDER_CREATE_SCOPE,
                "1",
                "order-key-123"
        )).thenReturn(Optional.of(record));

        IdempotencyConflictException exception = assertThrows(
                IdempotencyConflictException.class,
                () -> orderService.create(request, "order-key-123")
        );

        assertEquals("Request với Idempotency-Key này đang được xử lý", exception.getMessage());
        verifyNoInteractions(inventoryClient);
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void createShouldReplaceExpiredRecord() {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1001L, 2);
        IdempotencyRecordEntity expiredRecord = new IdempotencyRecordEntity(
                ORDER_CREATE_SCOPE,
                "1",
                "order-key-123",
                "expired-request-hash",
                LocalDateTime.now().minusMinutes(1)
        );

        when(idempotencyRecordRepository.findByScopeAndOwnerIdAndIdempotencyKey(
                ORDER_CREATE_SCOPE,
                "1",
                "order-key-123"
        )).thenReturn(Optional.of(expiredRecord));
        when(idempotencyRecordRepository.saveAndFlush(any(IdempotencyRecordEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.create(request, "order-key-123");

        verify(idempotencyRecordRepository).delete(expiredRecord);
        verify(idempotencyRecordRepository).flush();
        verify(inventoryClient).reserve(1001L, 2);
        assertNotNull(response.orderNo());
    }

    @Test
    void getCheckoutShouldReturnStatusAndExpiration() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        OrderEntity entity = new OrderEntity(
                "ORD-123",
                1L,
                1001L,
                2,
                expiresAt
        );
        when(orderRepository.findByOrderNo("ORD-123"))
                .thenReturn(Optional.of(entity));

        CheckoutResponse response = orderService.getCheckout("ORD-123");

        assertEquals("ORD-123", response.orderNo());
        assertEquals(OrderStatus.PENDING_PAYMENT, response.status());
        assertEquals(expiresAt, response.expiresAt());
    }

    private IdempotencyRecordEntity completedRecord(
            CreateOrderRequest request,
            String idempotencyKey,
            OrderResponse response
    ) throws Exception {
        IdempotencyRecordEntity record = new IdempotencyRecordEntity(
                ORDER_CREATE_SCOPE,
                request.userId().toString(),
                idempotencyKey,
                requestHash(request),
                LocalDateTime.now().plusHours(24)
        );
        record.complete(objectMapper.writeValueAsString(response));

        return record;
    }

    private String requestHash(CreateOrderRequest request) throws Exception {
        String rawRequest = request.userId() + "|" + request.eventId() + "|" + request.quantity();
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(rawRequest.getBytes(StandardCharsets.UTF_8));

        return HexFormat.of().formatHex(hash);
    }
}
