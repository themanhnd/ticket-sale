package com.ticketsale.common.outbox;

public enum OutboxStatus {
    PENDING, // mới được ghi vào db , chưa publish kafka
    RETRY, //  publish lỗi tạm thời , sẽ thử lại
    PUBLISHED,// đã publish thành công
    FAILED // lỗi quá số lần cho phép , cần admin xử lý
}
