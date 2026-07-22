package com.ticketsale.order.service;

import com.ticketsale.order.controller.dto.request.CreateOrderRequest;
import com.ticketsale.order.controller.dto.response.CheckoutResponse;
import com.ticketsale.order.controller.dto.response.OrderResponse;

public interface OrderService {

    OrderResponse create(CreateOrderRequest request);

    OrderResponse getByOrderNo(String orderNo);

    CheckoutResponse getCheckout(String orderNo);
}