package com.ticketsale.common.idempotency;
//Ta tách 2 loại idempotency:
//        API_REQUEST
//Cho HTTP request, ví dụ: user click đặt vé 3 lần
//        MESSAGE_CONSUMER
//Cho Kafka/event consumer, ví dụ: payment.completed đến 2 lần
public enum IdempotencyScope {
    API_REQUEST,
    MESSAGE_CONSUMER
}