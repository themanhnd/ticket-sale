# Kế hoạch công việc - Ticket Sale Microservices

## 1. Mục tiêu cuối cùng

Xây dựng lại thủ công một hệ thống bán vé theo kiến trúc microservices, chạy được ở local và Docker local, sau đó mở rộng dần theo luồng nghiệp vụ thật:

- người dùng xem sự kiện
- kiểm tra tồn kho vé
- tạo order
- thanh toán
- hết hạn thanh toán thì tự hủy order
- giải phóng vé đã giữ
- các service giao tiếp an toàn qua event

Mục tiêu học đi kèm:

- hiểu vai trò từng service nền
- hiểu cách config tập trung hoạt động
- hiểu service discovery và gateway route
- hiểu cách tách domain service
- hiểu vì sao cần Flyway, idempotency, outbox, consumer idempotent
- hiểu cách đi từ project nhỏ đến kiến trúc production-ready hơn

---

## 2. Nguyên tắc làm dự án

- ưu tiên chạy ổn ở local và Docker local trước
- mỗi phase phải có đầu ra kiểm chứng được
- build/test pass rồi mới sang bước sau
- thêm service theo nhu cầu domain, không thêm cho đủ số lượng
- config thay đổi thì ưu tiên restart service ở giai đoạn hiện tại
- production sẽ làm sau, không tối ưu sớm khi local chưa vững

---

## 3. Tổng quan các phase

| Phase | Tên | Mục tiêu |
|---|---|---|
| 1 | Platform base | Dựng khung multi-module và service nền |
| 1.5 | Service template | Tạo mẫu service chuẩn để clone nhanh |
| 2 | User service | Dựng service nghiệp vụ đầu tiên |
| 3 | Config + Discovery + Gateway flow | Nối các service nền thành luồng hoàn chỉnh |
| 4 | Docker local platform | Chạy full stack bằng Docker Compose |
| 5 | Env và cấu hình local | Làm sạch cách chạy local và Docker local |
| 6 | Event service | Dựng domain service thứ hai |
| 7 | Inventory service | Quản lý tồn kho vé |
| 8 | Order service | Tạo order và quản lý vòng đời order |
| 9 | Idempotency API | Chống double click / request lặp |
| 10 | Payment service | Mô phỏng thanh toán |
| 11 | Kafka + Outbox | Giao tiếp event an toàn hơn |
| 12 | Consumer idempotent | Chịu được at-least-once delivery |
| 13 | Timeout + release inventory | Hủy order hết hạn và trả lại vé |
| 14 | Observability cơ bản | Metrics, health, logs, dashboard cơ bản |
| 15 | Hardening và tài liệu | Dọn nợ kỹ thuật, chuẩn hóa, ghi docs |

---

## 4. Chi tiết từng phase

## Phase 1 - Platform base

### Mục tiêu

Dựng nền monorepo Maven multi-module để chứa các service.

### Thành phần

- root Maven project
- `common`
- `discovery`
- `config`
- `gateway`

### Done khi

- root build được
- từng module chạy độc lập được
- không lỗi dependency cha/con

### Checklist

- [x] Tạo root `pom.xml`
- [x] Tạo module `common`
- [x] Tạo module `discovery`
- [x] Tạo module `config`
- [x] Tạo module `gateway`
- [x] Sửa lỗi Maven module path/dependency

### Trạng thái hiện tại

- `DONE`

---

## Phase 1.5 - Service template

### Mục tiêu

Tạo một service mẫu đủ chuẩn để clone nhanh cho các domain service sau này.

### Thành phần

- Spring Boot web
- validation
- JPA
- Flyway
- actuator
- Eureka client
- test controller cơ bản
- Dockerfile

### Done khi

- `service-template` build/test pass
- có migration mẫu
- có API mẫu
- có Dockerfile chạy được

### Checklist

- [x] Tạo module `service-template`
- [x] Thêm migration base
- [x] Thêm controller/service/repository mẫu
- [x] Thêm test web cơ bản
- [x] Sửa lỗi compile/test của template

### Trạng thái hiện tại

- `DONE`

---

## Phase 2 - User service

### Mục tiêu

Clone từ template thành service nghiệp vụ đầu tiên: `user-service`.

### Domain

- user có `email`, `fullName`

### API

- `POST /api/users`
- `GET /api/users/{id}`

### Done khi

- `user-service` build/test pass
- chạy local được
- đi qua gateway được
- chạy Docker local được

### Checklist

- [x] Clone `service-template` -> `user-service`
- [x] Đổi package/class/domain
- [x] Tạo migration `users`
- [x] Sửa request/response/repository/service/controller
- [x] Thêm config repo cho `user-service`
- [x] Thêm route gateway cho `/api/users/**`
- [x] Chạy local thành công
- [x] Chạy Docker local thành công

### Trạng thái hiện tại

- `DONE`

---

## Phase 3 - Config + Discovery + Gateway flow

### Mục tiêu

Hiểu và chạy được luồng service lấy config, đăng ký Eureka, gateway route theo `lb://SERVICE-NAME`.

### Kiến thức cần nắm

- `config-service` cấp config tập trung
- `discovery` giữ danh bạ service
- `gateway` route request vào service đích
- service tự lấy config rồi tự đăng ký chính nó vào discovery

### Done khi

- `config` chạy ổn
- `discovery` chạy ổn
- `gateway` gọi được `user-service` qua Eureka

### Checklist

- [x] Tạo config repo `dev`/`docker`
- [x] Hiểu `spring.config.import`
- [x] Hiểu `lb://USER-SERVICE`
- [x] Gateway route hoạt động
- [x] Eureka hiển thị service đăng ký

### Trạng thái hiện tại

- `DONE`

---

## Phase 4 - Docker local platform

### Mục tiêu

Chạy full stack bằng Docker Compose local, không cần bật tay từng service.

### Thành phần

- `mysql`
- `redis`
- `discovery`
- `config`
- `gateway`
- `user-service`

### Sub-phase

#### 4.1 - Dựng Compose base
- [x] Có `docker-compose.yml`
- [x] Build image cho từng module
- [x] Up platform local được

#### 4.2 - Startup order
- [x] `depends_on` hợp lý
- [x] `config` có healthcheck
- [x] `mysql` có healthcheck
- [x] `user-service` chờ config/mysql

#### 4.3 - Readiness hoàn chỉnh
- [x] `user-service` có healthcheck
- [x] `gateway` có healthcheck
- [x] `gateway` chờ backend healthy

### Done khi

- `docker compose --profile platform ps` thấy service nền `healthy`
- gọi được API qua gateway trong Docker local

### Trạng thái hiện tại

- `DONE`

---

## Phase 5 - Env và cấu hình local

### Mục tiêu

Làm sạch cách quản lý biến môi trường để local và Docker local dễ theo dõi.

### Thành phần

- `.env`
- `.env.example`
- `.env.dev.example`
- `.env.prod.example`
- `docs/runbook-docker.md`

### Done khi

- `docker-compose.yml` không hardcode nhiều giá trị
- có file mẫu env
- local chỉ cần copy env mẫu rồi chạy

### Checklist

- [x] Tách biến MySQL/Redis/port khỏi compose
- [x] Thêm `.env.example`
- [x] Thêm `.env.dev.example`
- [x] Thêm `.env.prod.example`
- [x] Cập nhật `.gitignore`
- [x] Viết runbook Docker

### Trạng thái hiện tại

- `DONE`

---

## Phase 6 - Event service

### Mục tiêu

Dựng domain service thứ hai để quản lý sự kiện.

### Domain

- `code`
- `name`
- `location`

### API

- `POST /api/events`
- `GET /api/events/{id}`
- `GET /api/events`

### Checklist

#### 6.1 - Code service
- [x] Clone `service-template` -> `event-service`
- [x] Đổi package/class/domain
- [x] Sửa lỗi copy sót `template`
- [x] Thêm test controller
- [x] `mvn -pl event-service clean test` pass

#### 6.2 - Config và migration
- [x] `event-service-dev.yml`
- [x] `event-service-docker.yml`
- [x] migration bảng `events`

#### 6.3 - Gateway + Docker
- [x] route `/api/events/**`
- [x] Dockerfile `event-service`
- [x] thêm `event-service` vào compose
- [x] thêm `spring-cloud-starter-config`

#### 6.4 - Chạy thực tế
- [x] Docker local up thành công
- [x] Test API qua gateway
- [x] Test local non-docker
- [x] Review ngày 2026-07-07: `event-service` test pass, Docker daemon chưa bật nên chưa verify API runtime lại được

### Done khi

- `ticket-event-service` healthy
- Eureka có `EVENT-SERVICE`
- API event chạy qua gateway
- local run không Docker cũng chạy được

### Trạng thái hiện tại

- `DONE`

---

## Phase 7 - Inventory service

### Mục tiêu

Quản lý số lượng vé của từng event.

### Domain

- `eventId`
- `totalQuantity`
- `availableQuantity`

### API

- `POST /api/inventories`
- `GET /api/inventories/{eventId}`
- `POST /api/inventories/{eventId}/reserve`
- `POST /api/inventories/{eventId}/release`

### Checklist

- [x] Clone `service-template` -> `inventory-service`
- [x] Đổi package/class/domain sang `inventory`
- [x] Tạo migration `inventories`
- [x] Thêm API create/get
- [x] Thêm config repo
- [x] Thêm route gateway
- [x] Thêm Dockerfile
- [x] Thêm Docker Compose
- [x] Thêm `spring-cloud-starter-config`
- [x] Sửa `.dockerignore` để jar `inventory-service` vào build context
- [x] Build/test module pass
- [x] Docker local up healthy
- [x] Test API qua gateway
- [x] Test API gọi trực tiếp service
- [x] Thêm reserve/release cơ bản
- [ ] Test local non-docker (để sau)

### Bằng chứng đã verify

- `mvn -pl inventory-service clean test` pass: 8 tests, 0 failures, 0 errors
- `ticket-inventory-service` healthy
- `ticket-gateway` healthy
- Eureka register `INVENTORY-SERVICE` OK
- `POST http://localhost:8080/api/inventories` OK
- `GET http://localhost:8080/api/inventories/{eventId}` OK
- `GET http://localhost:8093/api/inventories/{eventId}` OK
- `POST http://localhost:8080/api/inventories/{eventId}/reserve` OK: 10 vé -> giữ 2 -> còn 8
- `POST http://localhost:8080/api/inventories/{eventId}/release` OK: còn 8 -> trả 1 -> còn 9
- Reserve quá số vé trả HTTP 400: `Không đủ vé để giữ chỗ`

### Nợ kỹ thuật để sau

- Test local non-docker cho `inventory-service`
- Idempotency cho reserve/release khi Order service gọi lặp
- Reservation record để biết vé đang giữ thuộc order nào
- Timeout release khi order hết hạn thanh toán

### Done khi

- tạo tồn kho được
- đọc tồn kho được
- reserve/release cơ bản chạy được
- chặn oversell cơ bản bằng DB lock được
- Docker local chạy end-to-end qua gateway được

### Trạng thái hiện tại

- `DONE`

---
## Phase 8 - Order service

### Mục tiêu

Tạo order giữ vé và quản lý trạng thái order chờ thanh toán.

### Domain V1

- `orderNo`
- `userId`
- `eventId`
- `quantity`
- `status`
- `expiresAt`

### API V1

- `POST /api/orders`
- `GET /api/orders/{orderNo}`
- `GET /api/orders/{orderNo}/checkout`

### Checklist

- [x] Chốt thiết kế Order V1
- [x] Tạo module `order-service`
- [x] Đổi package và application name sang `order`
- [x] Đổi tên class/file Template sang Order
- [x] Đổi logic domain từ template sang order
- [x] Migration `orders`
- [x] API create/get order
- [x] Nối order với inventory reserve
- [x] Endpoint checkout status
- [x] Thêm config/gateway/docker
- [x] Test local/Docker

### Hiểu nhanh Phase 8

- Bài giải thích chi tiết: [`docs/learning/phase-8-config-eureka-gateway.md`](learning/phase-8-config-eureka-gateway.md).
- `application.yml` chỉ giữ phần cố định: tên service, port, `configserver`.
- File `order-service-dev.yml` và `order-service-docker.yml` giữ khác biệt local/Docker như MySQL, Eureka, Inventory.
- Gateway nhận `POST /api/orders` và `GET /api/orders/**`, rồi chuyển tới `lb://ORDER-SERVICE`.
- `OrderServiceImpl.create()` giữ vé ở inventory trước, rồi mới lưu order.
- `GET /api/orders/{orderNo}/checkout` chỉ trả dữ liệu frontend cần: `orderNo`, `status`, `expiresAt`.
- Verify xong Phase 8: chạy local, chạy Docker, tạo order qua Gateway, checkout OK, thiếu vé trả `400`.

### Nợ kỹ thuật dự kiến

- Idempotency cho `POST /api/orders`.
- Outbox và event-driven flow.
- Payment service.
- Timeout cancel và release inventory.
- Consumer idempotent.

### Trạng thái hiện tại

- `DONE`

---

## Phase 9 - Idempotency API

### Mục tiêu

Tránh double click tạo nhiều order.

### Checklist

- [ ] Thiết kế `Idempotency-Key`
- [ ] Lưu idempotency record
- [ ] Trả response cũ nếu request trùng
- [ ] Áp dụng cho `POST /api/orders`
- [ ] test request lặp

### Done khi

- gửi 2 request cùng key chỉ tạo 1 order

### Trạng thái hiện tại

- `TODO`

---

## Phase 10 - Payment service

### Mục tiêu

Mô phỏng thanh toán thành công/thất bại.

### API gợi ý

- `POST /api/payments`
- `POST /api/payments/{paymentId}/complete`
- `POST /api/payments/{paymentId}/fail`

### Checklist

- [ ] Tạo `payment-service`
- [ ] migration payments
- [ ] API create payment
- [ ] API complete/fail payment
- [ ] config/gateway/docker
- [ ] test local/docker

### Done khi

- order có thể đi sang bước thanh toán giả lập

### Trạng thái hiện tại

- `TODO`

---

## Phase 11 - Kafka + Outbox

### Mục tiêu

Tách giao tiếp liên service qua event, giảm coupling và xử lý an toàn hơn.

### Event gợi ý

- `order.created`
- `inventory.reserved`
- `payment.completed`
- `payment.failed`
- `order.confirmed`
- `order.expired`

### Checklist

- [ ] Bật Kafka local
- [ ] outbox table dùng lại được
- [ ] publisher job / scheduler
- [ ] order-service ghi DB + outbox cùng transaction
- [ ] payment-service publish event kết quả
- [ ] test publish/consume local

### Done khi

- DB update và publish event không còn gắn cứng trong cùng logic sync

### Trạng thái hiện tại

- `TODO`

---

## Phase 12 - Consumer idempotent

### Mục tiêu

Chịu được Kafka at-least-once delivery.

### Checklist

- [ ] idempotent consumer cho `order.created`
- [ ] idempotent consumer cho `payment.completed`
- [ ] idempotent consumer cho `payment.failed`
- [ ] idempotent consumer cho `order.confirmed`
- [ ] test consume trùng message

### Done khi

- consume trùng không làm hỏng trạng thái business

### Trạng thái hiện tại

- `TODO`

---

## Phase 13 - Timeout payment + release inventory

### Mục tiêu

Nếu user không thanh toán trong X phút thì order tự hủy và vé được trả lại.

### Checklist

- [ ] đặt `expiresAt` khi tạo order
- [ ] scheduler scan order hết hạn
- [ ] đổi trạng thái `EXPIRED` / `CANCELLED`
- [ ] publish `order.expired`
- [ ] inventory-service release lại vé
- [ ] test end-to-end timeout

### Done khi

- order không thanh toán sẽ tự hết hạn
- inventory được trả lại đúng

### Trạng thái hiện tại

- `TODO`

---

## Phase 14 - Observability cơ bản

### Mục tiêu

Biết hệ đang sống hay chết, chỗ nào chậm, chỗ nào lỗi.

### Checklist

- [ ] chuẩn hóa actuator health
- [ ] metrics cho request count / error count
- [ ] prometheus scrape local
- [ ] dashboard cơ bản
- [ ] log correlation id cơ bản

### Done khi

- nhìn được health/metrics của từng service

### Trạng thái hiện tại

- `TODO`

---

## Phase 15 - Hardening và tài liệu

### Mục tiêu

Dọn nợ kỹ thuật, chuẩn hóa cách mở rộng service tiếp.

### Hạng mục

- Flyway chuẩn cho mọi service
- service-template đồng nhất hơn
- base outbox abstraction dùng lại được
- base idempotency abstraction cho API/consumer
- contract test cho event
- replay failed outbox pattern
- docs/adr đầy đủ hơn

### Checklist

- [ ] chuẩn hóa template cho service mới
- [ ] thêm docs ADR cần thiết
- [ ] ghi runbook local/dev
- [ ] note việc cần làm tiếp sau phase chính
- [ ] review encoding/comment/docs

### Done khi

- project dễ mở rộng, dễ dạy lại, dễ tiếp tục sau này

### Trạng thái hiện tại

- `TODO`

---

## 5. Thứ tự ưu tiên thực tế từ bây giờ

### Ưu tiên ngay

1. chốt `Phase 6 - Event service`
2. sang `Phase 7 - Inventory service`
3. sang `Phase 8 - Order service`
4. làm `Phase 9 - Idempotency API`
5. làm `Phase 10 - Payment service`
6. làm `Phase 11 - Kafka + Outbox`
7. làm `Phase 12 - Consumer idempotent`
8. làm `Phase 13 - Timeout + release inventory`

### Chưa ưu tiên lúc này

- production deploy chuẩn
- CI/CD
- cloud infra
- Docker secrets
- Kubernetes
- auto refresh config nâng cao

---

## 6. Cách tự theo dõi tiến độ

Mỗi khi xong một phase hoặc sub-phase, cập nhật 3 thứ:

1. trạng thái
- `TODO`
- `IN PROGRESS`
- `DONE`

2. bằng chứng
- lệnh nào pass
- API nào test pass
- service nào healthy

3. nợ kỹ thuật còn lại
- chưa test local non-docker
- chưa thêm healthcheck
- chưa thêm retry/outbox
- chưa xử lý idempotency

---

## 7. Trạng thái hiện tại toàn dự án

### Đã xong

- [x] Phase 1
- [x] Phase 1.5
- [x] Phase 2
- [x] Phase 3
- [x] Phase 4
- [x] Phase 5
- [x] Phase 6
- [x] Phase 7
- [x] Phase 8

### Đang làm


### Chưa làm

- [ ] Phase 9
- [ ] Phase 10
- [ ] Phase 11
- [ ] Phase 12
- [ ] Phase 13
- [ ] Phase 14
- [ ] Phase 15

---

## 8. Bước tiếp theo ngay bây giờ

1. Chốt thiết kế `Idempotency-Key` cho `POST /api/orders`.
2. Tạo migration lưu idempotency record.
3. Trả response cũ khi client gửi lại cùng key.
4. Viết test request trùng không tạo thêm order.