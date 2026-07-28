-- Lưu trạng thái xử lý request để chống tạo order nhiều lần khi client gửi lặp.
create table idempotency_records
(
    -- ID nội bộ của record.
    id              bigint primary key auto_increment,

    -- Tên nghiệp vụ sử dụng key, ví dụ ORDER_CREATE.
    scope           varchar(50)  not null,

    -- Người sở hữu key. Hiện tại là userId, sau này lấy từ JWT.
    owner_id        varchar(100) not null,

    -- Key do client gửi qua header Idempotency-Key.
    idempotency_key varchar(200) not null,

    -- SHA-256 của request body, dùng phát hiện cùng key nhưng body khác.
    -- SHA-256 dạng hex luôn dài 64 ký tự.
    request_hash    varchar(64)  not null,

    -- Trạng thái xử lý: PROCESSING hoặc COMPLETED.
    status          varchar(50)  not null,

    -- Response JSON của order đã tạo.
    -- Có thể null khi request còn ở trạng thái PROCESSING.
    response_body   text null,

    -- Thời điểm bắt đầu xử lý request.
    created_at      datetime     not null,

    -- Thời điểm key hết hiệu lực, hiện tại dự kiến created_at + 24 giờ.
    expires_at      datetime     not null,

    -- Một user chỉ được dùng một key một lần trong cùng nghiệp vụ.
    -- Đây là lớp bảo vệ cuối cùng khi hai request đến đồng thời.
    unique key uk_idempotency_records_scope_owner_key (
        scope,
        owner_id,
        idempotency_key
        )
);