package com.ticketsale.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketsale.order.controller.dto.response.OrderResponse;
import com.ticketsale.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService OrderService;

    @Test
    void createShouldReturnTemplate() throws Exception {
        Mockito.when(OrderService.create(any()))
                .thenReturn(new OrderResponse(
                        1L,
                        "DEMO",
                        "Demo template",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/api/templates")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TestRequest("DEMO", "Demo template"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private record TestRequest(String code, String name) {
    }
}