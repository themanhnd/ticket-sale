package com.ticketsale.order.service.impl;

import com.ticketsale.order.client.InventoryClient;
import com.ticketsale.order.controller.dto.request.CreateOrderRequest;
import com.ticketsale.order.controller.dto.response.OrderResponse;
import com.ticketsale.order.repository.OrderRepository;
import com.ticketsale.order.repository.entity.OrderEntity;
import com.ticketsale.order.repository.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

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
    private final OrderServiceImpl orderService = new OrderServiceImpl(
            orderRepository,
            inventoryClient
    );

    @Test
    void createShouldReserveInventoryBeforeSavingOrder() {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1001L, 2);
        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.create(request);

        InOrder callOrder = inOrder(inventoryClient, orderRepository);
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
        doThrow(new IllegalArgumentException("Không đủ vé để giữ chỗ"))
                .when(inventoryClient)
                .reserve(1001L, 99);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.create(request)
        );

        assertEquals("Không đủ vé để giữ chỗ", exception.getMessage());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }
}