package com.ticketsale.inventory.controller;

import com.ticketsale.common.response.ApiResponse;
import com.ticketsale.inventory.controller.dto.request.CreateInventoryRequest;
import com.ticketsale.inventory.controller.dto.response.InventoryResponse;
import com.ticketsale.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventories")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ApiResponse<InventoryResponse> create(@Valid @RequestBody CreateInventoryRequest request) {
        return ApiResponse.ok(inventoryService.create(request));
    }

    @GetMapping("/{eventId}")
    public ApiResponse<InventoryResponse> getByEventId(@PathVariable Long eventId) {
        return ApiResponse.ok(inventoryService.getByEventId(eventId));
    }
}