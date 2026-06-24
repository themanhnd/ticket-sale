package com.ticketsale.common.outbox;
import java.time.LocalDateTime;
//Mô tả : Nó là object mô tả 1 event cần publish

public record OutboxMessage(
        String aggregateType, // ví dụ : order, payment, inventory
        String aggregateId, // ví dụ : ORD-1 , PAY-100
        String topic, // kafka topic . Vi dụ : order.placed
        String eventKey,//Key để partition kafka , ví dụ : ORD-1
        String eventType,// Tên loại event, ví dụ: OrderPlacedEvent
        String payload,// JSON strong của event
        OutboxStatus status,// trạng thái
        int attemptCount,// Đếm số lần retry
        LocalDateTime nextAttemptAt //Khi nào publisher nên thử lại
) {

    public static OutboxMessage pending(
            String aggregateType,
            String aggregateId,
            String topic,
            String eventKey,
            String eventType,
            String payload
    ) {
        return new OutboxMessage(
                aggregateType,
                aggregateId,
                topic,
                eventKey,
                eventType,
                payload,
                OutboxStatus.PENDING,
                0,
                LocalDateTime.now()
        );
    }
}