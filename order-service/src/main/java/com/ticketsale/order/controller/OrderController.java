package com.ticketsale.order.controller;

import com.ticketsale.common.response.ApiResponse;
import com.ticketsale.order.controller.dto.request.CreateOrderRequest;
import com.ticketsale.order.controller.dto.response.OrderResponse;
import com.ticketsale.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

// API mẫu cho service clone từ service-template.
@RestController
@RequestMapping("/api/templates")
public class OrderController {

    private final OrderService OrderService;

    public OrderController(OrderService OrderService) {
        this.OrderService = OrderService;
    }

    // Tạo template mới.
    @PostMapping
    public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(OrderService.create(request));
    }

    // Lấy template theo id.
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(OrderService.getById(id));
    }
}