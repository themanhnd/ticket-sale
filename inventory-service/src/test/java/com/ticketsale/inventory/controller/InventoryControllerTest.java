package com.ticketsale.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketsale.inventory.controller.dto.response.InventoryResponse;
import com.ticketsale.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    @Test
    void createShouldReturnInventory() throws Exception {
        Mockito.when(inventoryService.create(any()))
                .thenReturn(new InventoryResponse(
                        1L,
                        1L,
                        100,
                        100,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/api/inventories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateTestRequest(1L, 100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value(1))
                .andExpect(jsonPath("$.data.totalQuantity").value(100));
    }

    @Test
    void reserveShouldReturnUpdatedInventory() throws Exception {
        Mockito.when(inventoryService.reserve(eq(1L), eq(2)))
                .thenReturn(new InventoryResponse(
                        1L,
                        1L,
                        100,
                        98,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/api/inventories/1/reserve")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ChangeTestRequest(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableQuantity").value(98));
    }

    @Test
    void releaseShouldReturnUpdatedInventory() throws Exception {
        Mockito.when(inventoryService.release(eq(1L), eq(2)))
                .thenReturn(new InventoryResponse(
                        1L,
                        1L,
                        100,
                        100,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/api/inventories/1/release")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ChangeTestRequest(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableQuantity").value(100));
    }

    private record CreateTestRequest(Long eventId, Integer totalQuantity) {
    }

    private record ChangeTestRequest(Integer quantity) {
    }
}