# Thiết kế Idempotency cho POST /api/orders

## Mục tiêu

Ngăn một thao tác đặt vé bị gửi lặp làm giữ vé và tạo nhiều order.

## Hợp đồng API

- `POST /api/orders` bắt buộc có header `Idempotency-Key`.
- Header thiếu hoặc chỉ chứa khoảng trắng trả `400 Bad Request`.
- Một thao tác mới phải dùng key mới.
- Chủ sở hữu key là `userId` trong request.

## Cách nhận diện request lặp

- Scope cố định: `ORDER_CREATE`.
- Khóa duy nhất trong DB: `scope + owner_id + idempotency_key`.
- Request được băm SHA-256 từ `userId|eventId|quantity`.
- Cùng key và cùng request hash trả lại `OrderResponse` đã lưu.
- Cùng key nhưng khác request hash trả `409 Conflict`.

## Dữ liệu lưu

Bảng `idempotency_records` lưu scope, owner, key, request hash, trạng thái xử lý, JSON response, thời gian tạo và thời gian hết hạn. Record tồn tại 24 giờ.

## Luồng xử lý

1. Tìm record còn hiệu lực theo scope, owner và key.
2. Nếu record đã hoàn tất, kiểm tra hash rồi trả response cũ.
3. Nếu record đang xử lý, không chạy lại nghiệp vụ và trả `409 Conflict`.
4. Nếu chưa có record, tạo record `PROCESSING` trước khi reserve inventory.
5. Reserve inventory, lưu order, serialize response và đổi record thành `COMPLETED` trong cùng transaction DB của order-service.
6. Nếu nghiệp vụ lỗi, transaction rollback record để client có thể thử lại.

## Giới hạn Phase 9

- Chưa có job dọn record hết hạn; record hết hạn được xóa khi key đó được dùng lại.
- Transaction DB không bao phủ HTTP call sang inventory-service. Phase sau cần saga/outbox và thao tác release để bù khi lỗi liên service.

## Kiểm thử bắt buộc

- Thiếu `Idempotency-Key` trả `400`.
- Request đầu tiên reserve inventory và tạo một order.
- Gửi lại cùng key, cùng body trả response cũ và không reserve lần hai.
- Gửi lại cùng key, khác body trả `409`.
