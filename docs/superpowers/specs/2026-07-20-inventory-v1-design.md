# Inventory v1 Design

## Mục tiêu

Dựng `inventory-service` v1 để quản lý tồn kho vé theo event và chặn oversell trong 1 database.

## Phạm vi

### Có trong v1

- Tạo inventory cho event.
- Xem inventory theo `eventId`.
- Reserve vé bằng transaction + pessimistic lock.
- Release vé bằng transaction + pessimistic lock.
- Chạy được qua local, Docker, và gateway.

### Không có trong v1

- Idempotency key.
- Reservation record riêng.
- Kafka / outbox.
- Consumer idempotent.
- Timeout tự động release.
- Optimistic lock / retry.
- Audit lịch sử reserve/release.

## API

### Tạo inventory

`POST /api/inventories`

Body:

```json
{
  "eventId": 1,
  "totalQuantity": 100
}
```

### Xem inventory

`GET /api/inventories/{eventId}`

### Reserve vé

`POST /api/inventories/{eventId}/reserve`

Body:

```json
{
  "quantity": 2
}
```

### Release vé

`POST /api/inventories/{eventId}/release`

Body:

```json
{
  "quantity": 2
}
```

## Luật nghiệp vụ

### Create

- `eventId` phải chưa có inventory.
- `totalQuantity` phải > 0.
- `availableQuantity = totalQuantity` khi tạo.

### Reserve

- `quantity` phải > 0.
- inventory phải tồn tại.
- `availableQuantity >= quantity`.
- nếu đủ vé, trừ `availableQuantity` theo `quantity`.
- nếu không đủ vé, trả lỗi 400.

### Release

- `quantity` phải > 0.
- inventory phải tồn tại.
- `availableQuantity + quantity <= totalQuantity`.
- nếu hợp lệ, cộng `availableQuantity` theo `quantity`.
- nếu vượt tổng vé, trả lỗi 400.

## Transaction strategy

Dùng `PESSIMISTIC_WRITE` trên row inventory theo `eventId`.

Lý do:

- chặn 2 request cùng đọc cùng lúc rồi trừ vé sai.
- dễ hiểu hơn optimistic lock ở phase này.
- đủ để chống oversell trong 1 DB local/Docker.

## Data model

Table `inventories`:

- `id`
- `event_id` unique
- `total_quantity`
- `available_quantity`
- `created_at`
- `updated_at`

## Service contract

### InventoryService

- `create(CreateInventoryRequest request)`
- `getByEventId(Long eventId)`
- `reserve(Long eventId, Integer quantity)`
- `release(Long eventId, Integer quantity)`

### InventoryEntity

- giữ state `totalQuantity` và `availableQuantity`
- có method nội bộ để trừ / cộng vé
- tự bảo vệ không vượt giới hạn

## Error rules

- event inventory trùng: 400
- inventory không tồn tại: 400
- quantity <= 0: 400
- reserve quá số còn lại: 400
- release vượt tổng vé: 400

## Acceptance criteria

- tạo inventory được qua gateway và direct service.
- reserve trừ đúng `availableQuantity`.
- release cộng đúng `availableQuantity`.
- không oversell khi 2 request đồng thời.
- `mvn -pl inventory-service clean test` pass.
- Docker local stack pass.

## Lộ trình sau v1

Sau khi v1 chạy ổn, nâng cấp theo thứ tự:

1. reservation record.
2. idempotency cho order API.
3. timeout release.
4. Kafka / outbox.
5. idempotent consumer.
6. optimistic lock hoặc atomic SQL update nếu cần scale cao hơn.