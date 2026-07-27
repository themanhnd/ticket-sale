package com.ticketsale.order.controller;

import com.ticketsale.common.response.ApiResponse;
import com.ticketsale.order.controller.dto.request.CreateOrderRequest;
import com.ticketsale.order.controller.dto.response.CheckoutResponse;
import com.ticketsale.order.controller.dto.response.OrderResponse;
import com.ticketsale.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Nhận request tạo order và bắt buộc client gửi key đại diện cho một thao tác đặt vé.
    @PostMapping
    public ApiResponse<OrderResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key không được để trống");
        }

        return ApiResponse.ok(orderService.create(request, idempotencyKey));
    }

    // Lấy order theo mã order public.
    @GetMapping("/{orderNo}")
    public ApiResponse<OrderResponse> getByOrderNo(
            @PathVariable String orderNo
    ) {
        return ApiResponse.ok(orderService.getByOrderNo(orderNo));
    }

    // Trả trạng thái checkout để frontend biết order còn thời gian thanh toán hay không.
    @GetMapping("/{orderNo}/checkout")
    public ApiResponse<CheckoutResponse> getCheckout(
            @PathVariable String orderNo
    ) {
        return ApiResponse.ok(orderService.getCheckout(orderNo));
    }
}