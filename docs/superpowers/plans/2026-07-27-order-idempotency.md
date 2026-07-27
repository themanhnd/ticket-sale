# Order Idempotency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bảo đảm `POST /api/orders` không tạo nhiều order khi client gửi lặp cùng thao tác.

**Architecture:** Controller nhận `Idempotency-Key`; order-service dùng MySQL record có unique key để chặn request trùng trước side effect. Response hoàn tất được lưu dạng JSON và phát lại cho request trùng.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring MVC, Spring Data JPA, MySQL 8.4, Flyway, Jackson, JUnit 5, Mockito.

## Global Constraints

- Không thêm dependency mới.
- Comment mới dùng tiếng Việt UTF-8.
- TTL idempotency là 24 giờ.
- Scope tạo order là `ORDER_CREATE`.

---

### Task 1: Khóa hợp đồng HTTP bằng test

**Files:**
- Modify: `order-service/src/test/java/com/ticketsale/order/controller/OrderControllerTest.java`
- Modify: `order-service/src/main/java/com/ticketsale/order/controller/OrderController.java`
- Modify: `order-service/src/main/java/com/ticketsale/order/service/OrderService.java`

- [ ] Viết test header hợp lệ được truyền vào `OrderService.create(request, key)`.
- [ ] Viết test thiếu header trả `400`.
- [ ] Chạy test và xác nhận fail vì controller chưa hỗ trợ header.
- [ ] Sửa API tối thiểu để test pass.

### Task 2: Tạo nơi lưu idempotency

**Files:**
- Create: `order-service/src/main/resources/db/migration/V2__create_idempotency_records_table.sql`
- Create: `order-service/src/main/java/com/ticketsale/order/repository/entity/IdempotencyRecordEntity.java`
- Create: `order-service/src/main/java/com/ticketsale/order/repository/entity/IdempotencyStatus.java`
- Create: `order-service/src/main/java/com/ticketsale/order/repository/IdempotencyRecordRepository.java`

- [ ] Tạo migration với unique key `scope, owner_id, idempotency_key`.
- [ ] Tạo entity có trạng thái `PROCESSING` và `COMPLETED`.
- [ ] Tạo repository tìm record theo scope, owner và key.

### Task 3: Áp dụng idempotency vào create order

**Files:**
- Modify: `order-service/src/test/java/com/ticketsale/order/service/impl/OrderServiceImplTest.java`
- Modify: `order-service/src/main/java/com/ticketsale/order/service/impl/OrderServiceImpl.java`
- Create: `order-service/src/main/java/com/ticketsale/order/exception/IdempotencyConflictException.java`
- Modify: `order-service/src/main/java/com/ticketsale/order/exception/GlobalExceptionHandler.java`

- [ ] Viết test request đầu tạo record, reserve và lưu order.
- [ ] Viết test request trùng trả response cũ, không chạy side effect.
- [ ] Viết test cùng key khác body ném conflict.
- [ ] Chạy test và xác nhận fail vì logic chưa tồn tại.
- [ ] Dùng SHA-256 từ `userId|eventId|quantity` để so request.
- [ ] Lưu response bằng `ObjectMapper` và đặt TTL 24 giờ.
- [ ] Map conflict thành HTTP `409`.

### Task 4: Xác minh và cập nhật roadmap

**Files:**
- Modify: `docs/ke-hoach-cong-viec.md`

- [ ] Chạy `mvn -pl order-service test`.
- [ ] Chạy build module và dependency liên quan.
- [ ] Đánh dấu checklist Phase 9 hoàn tất khi toàn bộ test pass.
