# Order V1 Design

**Date:** 2026-07-21

## Goal

Tạo `order-service` để ghi nhận order chờ thanh toán và giữ vé qua `inventory-service`.

## Scope

- Tạo order với `userId`, `eventId`, `quantity`.
- Gọi đồng bộ `inventory-service` để reserve vé.
- Chỉ lưu order sau khi reserve thành công.
- Trả `orderNo`, trạng thái và thời điểm hết hạn thanh toán.
- Cho phép đọc order theo `orderNo`.
- Chưa làm Kafka, Outbox, Idempotency, Payment thật và timeout scheduler.

## API

### Create order

```http
POST /api/orders
Content-Type: application/json
```

Request:

```json
{
  "userId": 1,
  "eventId": 1001,
  "quantity": 2
}
```

Response data:

```json
{
  "orderNo": "ORD-<uuid>",
  "userId": 1,
  "eventId": 1001,
  "quantity": 2,
  "status": "PENDING_PAYMENT",
  "expiresAt": "2026-07-21T23:45:00"
}
```

### Get order

```http
GET /api/orders/{orderNo}
```

### Checkout status

```http
GET /api/orders/{orderNo}/checkout
```

V1 trả `orderNo`, `status`, `expiresAt`. `queuePosition` và `paymentUrl` để Phase 10.

## Data model

Bảng `orders` gồm:

- `id` — khóa chính.
- `order_no` — mã order duy nhất.
- `user_id` — ID user.
- `event_id` — ID event.
- `quantity` — số vé.
- `status` — `PENDING_PAYMENT`.
- `expires_at` — thời điểm hết hạn thanh toán.
- `created_at` — thời điểm tạo.
- `updated_at` — thời điểm cập nhật.

## Create flow

1. Client gọi Gateway.
2. Gateway chuyển request tới `order-service`.
3. `order-service` validate request.
4. `order-service` gọi `POST /api/inventories/{eventId}/reserve`.
5. Inventory đủ vé thì trả kết quả thành công.
6. `order-service` lưu order với trạng thái `PENDING_PAYMENT`.
7. Trả order cho client.

## Failure flow

- Inventory không tồn tại hoặc không đủ vé: trả HTTP 400, không tạo order.
- Request quantity không hợp lệ: trả HTTP 400.
- Nếu reserve thành công nhưng lưu order lỗi, vé có thể bị giữ nhưng chưa có order. Đây là nợ kỹ thuật đã chấp nhận trong V1; Outbox/compensation sẽ xử lý ở phase sau.

## Technical choices

- Dùng Spring MVC và `RestClient`.
- URL inventory lấy từ config repo:
  - local: `http://localhost:8093`
  - Docker: `http://inventory-service:8093`
- Dùng Flyway, JPA, Eureka, Config Server và Gateway giống các service trước.
- `expiresAt` dùng 15 phút sau thời điểm tạo; chưa đưa thành config riêng trong V1.