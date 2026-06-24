package com.ticketsale.common.idempotency;

import java.time.Duration;
import java.util.Optional;

//Đây là abstraction cho nơi lưu idempotency state. Sau này implementation có thể là Redis, Db
public interface IdempotencyStore {
    //Tìm response đã có cho request lặp
    Optional<String> findResponse(IdempotencyKey key);
    //Lưu response cho request đó
    void saveResponse(IdempotencyKey key, String responsePayload, Duration ttl);
    //Đánh dấu event/message đã xử lý hoặc đang xử lý
    boolean markMessageProcessing(IdempotencyKey key, Duration ttl);
}
