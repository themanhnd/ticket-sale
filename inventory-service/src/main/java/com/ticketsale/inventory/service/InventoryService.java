package com.ticketsale.inventory.service;

import com.ticketsale.inventory.controller.dto.request.CreateInventoryRequest;
import com.ticketsale.inventory.controller.dto.response.InventoryResponse;

public interface InventoryService {

    InventoryResponse create(CreateInventoryRequest request);

    InventoryResponse getByEventId(Long eventId);
}