package com.ticketsale.order.service;

import com.ticketsale.order.controller.dto.request.CreateOrderRequest;
import com.ticketsale.order.controller.dto.response.OrderResponse;

// Khai báo nghiệp vụ của template service.
public interface OrderService {

    OrderResponse create(CreateOrderRequest request);

    OrderResponse getById(Long id);
}