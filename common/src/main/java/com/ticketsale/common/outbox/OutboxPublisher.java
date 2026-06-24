package com.ticketsale.common.outbox;

// Đây là interface chung cho "worker publish outbox"
public interface OutboxPublisher {
    //lấy các record đến hạn
    //publish theo batch
    //trả số lượng đã xử lý
    int publishDueMessages(int batchSize);
}
