package com.ticketsale.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketsale.order.controller.dto.response.CheckoutResponse;
import com.ticketsale.order.controller.dto.response.OrderResponse;
import com.ticketsale.order.repository.entity.OrderStatus;
import com.ticketsale.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = OrderController.class, properties = "spring.cloud.config.enabled=false")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void createShouldReturnOrder() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        Mockito.when(orderService.create(any()))
                .thenReturn(new OrderResponse(
                        1L,
                        "ORD-123",
                        1L,
                        1001L,
                        2,
                        OrderStatus.PENDING_PAYMENT,
                        now.plusMinutes(15),
                        now,
                        now
                ));

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new CreateTestRequest(1L, 1001L, 2)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNo").value("ORD-123"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));
    }

    @Test
    void createShouldRejectInvalidQuantity() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new CreateTestRequest(1L, 1001L, 0)
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getShouldReturnOrder() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        Mockito.when(orderService.getByOrderNo("ORD-123"))
                .thenReturn(new OrderResponse(
                        1L,
                        "ORD-123",
                        1L,
                        1001L,
                        2,
                        OrderStatus.PENDING_PAYMENT,
                        now.plusMinutes(15),
                        now,
                        now
                ));

        mockMvc.perform(get("/api/orders/ORD-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value("ORD-123"));
    }

    @Test
    void checkoutShouldReturnPaymentWaitingStatus() throws Exception {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        Mockito.when(orderService.getCheckout("ORD-123"))
                .thenReturn(new CheckoutResponse(
                        "ORD-123",
                        OrderStatus.PENDING_PAYMENT,
                        expiresAt
                ));

        mockMvc.perform(get("/api/orders/ORD-123/checkout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value("ORD-123"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.expiresAt").exists());
    }

    private record CreateTestRequest(
            Long userId,
            Long eventId,
            Integer quantity
    ) {
    }
}