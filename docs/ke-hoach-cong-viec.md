# Káº¿ hoáº¡ch cÃ´ng viá»‡c - Ticket Sale Microservices

## 1. Má»¥c tiÃªu cuá»‘i cÃ¹ng

XÃ¢y dá»±ng láº¡i thá»§ cÃ´ng má»™t há»‡ thá»‘ng bÃ¡n vÃ© theo kiáº¿n trÃºc microservices, cháº¡y Ä‘Æ°á»£c á»Ÿ local vÃ  Docker local, sau Ä‘Ã³ má»Ÿ rá»™ng dáº§n theo luá»“ng nghiá»‡p vá»¥ tháº­t:

- ngÆ°á»i dÃ¹ng xem sá»± kiá»‡n
- kiá»ƒm tra tá»“n kho vÃ©
- táº¡o order
- thanh toÃ¡n
- háº¿t háº¡n thanh toÃ¡n thÃ¬ tá»± há»§y order
- giáº£i phÃ³ng vÃ© Ä‘Ã£ giá»¯
- cÃ¡c service giao tiáº¿p an toÃ n qua event

Má»¥c tiÃªu há»c Ä‘i kÃ¨m:

- hiá»ƒu vai trÃ² tá»«ng service ná»n
- hiá»ƒu cÃ¡ch config táº­p trung hoáº¡t Ä‘á»™ng
- hiá»ƒu service discovery vÃ  gateway route
- hiá»ƒu cÃ¡ch tÃ¡ch domain service
- hiá»ƒu vÃ¬ sao cáº§n Flyway, idempotency, outbox, consumer idempotent
- hiá»ƒu cÃ¡ch Ä‘i tá»« project nhá» Ä‘áº¿n kiáº¿n trÃºc production-ready hÆ¡n

---

## 2. NguyÃªn táº¯c lÃ m dá»± Ã¡n

- Æ°u tiÃªn cháº¡y á»•n á»Ÿ local vÃ  Docker local trÆ°á»›c
- má»—i phase pháº£i cÃ³ Ä‘áº§u ra kiá»ƒm chá»©ng Ä‘Æ°á»£c
- build/test pass rá»“i má»›i sang bÆ°á»›c sau
- thÃªm service theo nhu cáº§u domain, khÃ´ng thÃªm cho Ä‘á»§ sá»‘ lÆ°á»£ng
- config thay Ä‘á»•i thÃ¬ Æ°u tiÃªn restart service á»Ÿ giai Ä‘oáº¡n hiá»‡n táº¡i
- production sáº½ lÃ m sau, khÃ´ng tá»‘i Æ°u sá»›m khi local chÆ°a vá»¯ng

---

## 3. Tá»•ng quan cÃ¡c phase

| Phase | TÃªn | Má»¥c tiÃªu |
|---|---|---|
| 1 | Platform base | Dá»±ng khung multi-module vÃ  service ná»n |
| 1.5 | Service template | Táº¡o máº«u service chuáº©n Ä‘á»ƒ clone nhanh |
| 2 | User service | Dá»±ng service nghiá»‡p vá»¥ Ä‘áº§u tiÃªn |
| 3 | Config + Discovery + Gateway flow | Ná»‘i cÃ¡c service ná»n thÃ nh luá»“ng hoÃ n chá»‰nh |
| 4 | Docker local platform | Cháº¡y full stack báº±ng Docker Compose |
| 5 | Env vÃ  cáº¥u hÃ¬nh local | LÃ m sáº¡ch cÃ¡ch cháº¡y local vÃ  Docker local |
| 6 | Event service | Dá»±ng domain service thá»© hai |
| 7 | Inventory service | Quáº£n lÃ½ tá»“n kho vÃ© |
| 8 | Order service | Táº¡o order vÃ  quáº£n lÃ½ vÃ²ng Ä‘á»i order |
| 9 | Idempotency API | Chá»‘ng double click / request láº·p |
| 10 | Payment service | MÃ´ phá»ng thanh toÃ¡n |
| 11 | Kafka + Outbox | Giao tiáº¿p event an toÃ n hÆ¡n |
| 12 | Consumer idempotent | Chá»‹u Ä‘Æ°á»£c at-least-once delivery |
| 13 | Timeout + release inventory | Há»§y order háº¿t háº¡n vÃ  tráº£ láº¡i vÃ© |
| 14 | Observability cÆ¡ báº£n | Metrics, health, logs, dashboard cÆ¡ báº£n |
| 15 | Hardening vÃ  tÃ i liá»‡u | Dá»n ná»£ ká»¹ thuáº­t, chuáº©n hÃ³a, ghi docs |

---

## 4. Chi tiáº¿t tá»«ng phase

## Phase 1 - Platform base

### Má»¥c tiÃªu

Dá»±ng ná»n monorepo Maven multi-module Ä‘á»ƒ chá»©a cÃ¡c service.

### ThÃ nh pháº§n

- root Maven project
- `common`
- `discovery`
- `config`
- `gateway`

### Done khi

- root build Ä‘Æ°á»£c
- tá»«ng module cháº¡y Ä‘á»™c láº­p Ä‘Æ°á»£c
- khÃ´ng lá»—i dependency cha/con

### Checklist

- [x] Táº¡o root `pom.xml`
- [x] Táº¡o module `common`
- [x] Táº¡o module `discovery`
- [x] Táº¡o module `config`
- [x] Táº¡o module `gateway`
- [x] Sá»­a lá»—i Maven module path/dependency

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `DONE`

---

## Phase 1.5 - Service template

### Má»¥c tiÃªu

Táº¡o má»™t service máº«u Ä‘á»§ chuáº©n Ä‘á»ƒ clone nhanh cho cÃ¡c domain service sau nÃ y.

### ThÃ nh pháº§n

- Spring Boot web
- validation
- JPA
- Flyway
- actuator
- Eureka client
- test controller cÆ¡ báº£n
- Dockerfile

### Done khi

- `service-template` build/test pass
- cÃ³ migration máº«u
- cÃ³ API máº«u
- cÃ³ Dockerfile cháº¡y Ä‘Æ°á»£c

### Checklist

- [x] Táº¡o module `service-template`
- [x] ThÃªm migration base
- [x] ThÃªm controller/service/repository máº«u
- [x] ThÃªm test web cÆ¡ báº£n
- [x] Sá»­a lá»—i compile/test cá»§a template

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `DONE`

---

## Phase 2 - User service

### Má»¥c tiÃªu

Clone tá»« template thÃ nh service nghiá»‡p vá»¥ Ä‘áº§u tiÃªn: `user-service`.

### Domain

- user cÃ³ `email`, `fullName`

### API

- `POST /api/users`
- `GET /api/users/{id}`

### Done khi

- `user-service` build/test pass
- cháº¡y local Ä‘Æ°á»£c
- Ä‘i qua gateway Ä‘Æ°á»£c
- cháº¡y Docker local Ä‘Æ°á»£c

### Checklist

- [x] Clone `service-template` -> `user-service`
- [x] Äá»•i package/class/domain
- [x] Táº¡o migration `users`
- [x] Sá»­a request/response/repository/service/controller
- [x] ThÃªm config repo cho `user-service`
- [x] ThÃªm route gateway cho `/api/users/**`
- [x] Cháº¡y local thÃ nh cÃ´ng
- [x] Cháº¡y Docker local thÃ nh cÃ´ng

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `DONE`

---

## Phase 3 - Config + Discovery + Gateway flow

### Má»¥c tiÃªu

Hiá»ƒu vÃ  cháº¡y Ä‘Æ°á»£c luá»“ng service láº¥y config, Ä‘Äƒng kÃ½ Eureka, gateway route theo `lb://SERVICE-NAME`.

### Kiáº¿n thá»©c cáº§n náº¯m

- `config-service` cáº¥p config táº­p trung
- `discovery` giá»¯ danh báº¡ service
- `gateway` route request vÃ o service Ä‘Ã­ch
- service tá»± láº¥y config rá»“i tá»± Ä‘Äƒng kÃ½ chÃ­nh nÃ³ vÃ o discovery

### Done khi

- `config` cháº¡y á»•n
- `discovery` cháº¡y á»•n
- `gateway` gá»i Ä‘Æ°á»£c `user-service` qua Eureka

### Checklist

- [x] Táº¡o config repo `dev`/`docker`
- [x] Hiá»ƒu `spring.config.import`
- [x] Hiá»ƒu `lb://USER-SERVICE`
- [x] Gateway route hoáº¡t Ä‘á»™ng
- [x] Eureka hiá»ƒn thá»‹ service Ä‘Äƒng kÃ½

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `DONE`

---

## Phase 4 - Docker local platform

### Má»¥c tiÃªu

Cháº¡y full stack báº±ng Docker Compose local, khÃ´ng cáº§n báº­t tay tá»«ng service.

### ThÃ nh pháº§n

- `mysql`
- `redis`
- `discovery`
- `config`
- `gateway`
- `user-service`

### Sub-phase

#### 4.1 - Dá»±ng Compose base
- [x] CÃ³ `docker-compose.yml`
- [x] Build image cho tá»«ng module
- [x] Up platform local Ä‘Æ°á»£c

#### 4.2 - Startup order
- [x] `depends_on` há»£p lÃ½
- [x] `config` cÃ³ healthcheck
- [x] `mysql` cÃ³ healthcheck
- [x] `user-service` chá» config/mysql

#### 4.3 - Readiness hoÃ n chá»‰nh
- [x] `user-service` cÃ³ healthcheck
- [x] `gateway` cÃ³ healthcheck
- [x] `gateway` chá» backend healthy

### Done khi

- `docker compose --profile platform ps` tháº¥y service ná»n `healthy`
- gá»i Ä‘Æ°á»£c API qua gateway trong Docker local

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `DONE`

---

## Phase 5 - Env vÃ  cáº¥u hÃ¬nh local

### Má»¥c tiÃªu

LÃ m sáº¡ch cÃ¡ch quáº£n lÃ½ biáº¿n mÃ´i trÆ°á»ng Ä‘á»ƒ local vÃ  Docker local dá»… theo dÃµi.

### ThÃ nh pháº§n

- `.env`
- `.env.example`
- `.env.dev.example`
- `.env.prod.example`
- `docs/runbook-docker.md`

### Done khi

- `docker-compose.yml` khÃ´ng hardcode nhiá»u giÃ¡ trá»‹
- cÃ³ file máº«u env
- local chá»‰ cáº§n copy env máº«u rá»“i cháº¡y

### Checklist

- [x] TÃ¡ch biáº¿n MySQL/Redis/port khá»i compose
- [x] ThÃªm `.env.example`
- [x] ThÃªm `.env.dev.example`
- [x] ThÃªm `.env.prod.example`
- [x] Cáº­p nháº­t `.gitignore`
- [x] Viáº¿t runbook Docker

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `DONE`

---

## Phase 6 - Event service

### Má»¥c tiÃªu

Dá»±ng domain service thá»© hai Ä‘á»ƒ quáº£n lÃ½ sá»± kiá»‡n.

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
- [x] Äá»•i package/class/domain
- [x] Sá»­a lá»—i copy sÃ³t `template`
- [x] ThÃªm test controller
- [x] `mvn -pl event-service clean test` pass

#### 6.2 - Config vÃ  migration
- [x] `event-service-dev.yml`
- [x] `event-service-docker.yml`
- [x] migration báº£ng `events`

#### 6.3 - Gateway + Docker
- [x] route `/api/events/**`
- [x] Dockerfile `event-service`
- [x] thÃªm `event-service` vÃ o compose
- [x] thÃªm `spring-cloud-starter-config`

#### 6.4 - Cháº¡y thá»±c táº¿
- [x] Docker local up thÃ nh cÃ´ng
- [x] Test API qua gateway
- [x] Test local non-docker
- [x] Review ngÃ y 2026-07-07: `event-service` test pass, Docker daemon chÆ°a báº­t nÃªn chÆ°a verify API runtime láº¡i Ä‘Æ°á»£c

### Done khi

- `ticket-event-service` healthy
- Eureka cÃ³ `EVENT-SERVICE`
- API event cháº¡y qua gateway
- local run khÃ´ng Docker cÅ©ng cháº¡y Ä‘Æ°á»£c

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

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
- [ ] Test local non-docker
- [ ] Thêm reserve/release cơ bản

### Bằng chứng đã verify

- `mvn -pl inventory-service clean test` pass
- `ticket-inventory-service` healthy
- `ticket-gateway` healthy
- Eureka register `INVENTORY-SERVICE` OK
- `POST http://localhost:8080/api/inventories` OK
- `GET http://localhost:8080/api/inventories/1` OK
- `GET http://localhost:8093/api/inventories/1` OK

### Done khi

- tạo tồn kho được
- đọc tồn kho được
- local non-docker chạy được
- reserve/release cơ bản chạy được

### Trạng thái hiện tại

- `IN PROGRESS`

---
## Phase 8 - Order service

### Má»¥c tiÃªu

Táº¡o order giá»¯ vÃ© vÃ  quáº£n lÃ½ tráº¡ng thÃ¡i order.

### Domain gá»£i Ã½

- `orderNo`
- `eventId`
- `userId`
- `quantity`
- `status`
- `expiresAt`

### API gá»£i Ã½

- `POST /api/orders`
- `GET /api/orders/{orderNo}`
- `GET /api/orders/{orderNo}/checkout`

### Checklist

- [ ] Táº¡o `order-service`
- [ ] Migration `orders`
- [ ] API create/get order
- [ ] endpoint checkout status
- [ ] thÃªm config/gateway/docker
- [ ] test local/docker

### Done khi

- táº¡o order Ä‘Æ°á»£c
- xem tráº¡ng thÃ¡i order Ä‘Æ°á»£c
- cÃ³ `expiresAt`

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `TODO`

---

## Phase 9 - Idempotency API

### Má»¥c tiÃªu

TrÃ¡nh double click táº¡o nhiá»u order.

### Checklist

- [ ] Thiáº¿t káº¿ `Idempotency-Key`
- [ ] LÆ°u idempotency record
- [ ] Tráº£ response cÅ© náº¿u request trÃ¹ng
- [ ] Ãp dá»¥ng cho `POST /api/orders`
- [ ] test request láº·p

### Done khi

- gá»­i 2 request cÃ¹ng key chá»‰ táº¡o 1 order

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `TODO`

---

## Phase 10 - Payment service

### Má»¥c tiÃªu

MÃ´ phá»ng thanh toÃ¡n thÃ nh cÃ´ng/tháº¥t báº¡i.

### API gá»£i Ã½

- `POST /api/payments`
- `POST /api/payments/{paymentId}/complete`
- `POST /api/payments/{paymentId}/fail`

### Checklist

- [ ] Táº¡o `payment-service`
- [ ] migration payments
- [ ] API create payment
- [ ] API complete/fail payment
- [ ] config/gateway/docker
- [ ] test local/docker

### Done khi

- order cÃ³ thá»ƒ Ä‘i sang bÆ°á»›c thanh toÃ¡n giáº£ láº­p

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `TODO`

---

## Phase 11 - Kafka + Outbox

### Má»¥c tiÃªu

TÃ¡ch giao tiáº¿p liÃªn service qua event, giáº£m coupling vÃ  xá»­ lÃ½ an toÃ n hÆ¡n.

### Event gá»£i Ã½

- `order.created`
- `inventory.reserved`
- `payment.completed`
- `payment.failed`
- `order.confirmed`
- `order.expired`

### Checklist

- [ ] Báº­t Kafka local
- [ ] outbox table dÃ¹ng láº¡i Ä‘Æ°á»£c
- [ ] publisher job / scheduler
- [ ] order-service ghi DB + outbox cÃ¹ng transaction
- [ ] payment-service publish event káº¿t quáº£
- [ ] test publish/consume local

### Done khi

- DB update vÃ  publish event khÃ´ng cÃ²n gáº¯n cá»©ng trong cÃ¹ng logic sync

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `TODO`

---

## Phase 12 - Consumer idempotent

### Má»¥c tiÃªu

Chá»‹u Ä‘Æ°á»£c Kafka at-least-once delivery.

### Checklist

- [ ] idempotent consumer cho `order.created`
- [ ] idempotent consumer cho `payment.completed`
- [ ] idempotent consumer cho `payment.failed`
- [ ] idempotent consumer cho `order.confirmed`
- [ ] test consume trÃ¹ng message

### Done khi

- consume trÃ¹ng khÃ´ng lÃ m há»ng tráº¡ng thÃ¡i business

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `TODO`

---

## Phase 13 - Timeout payment + release inventory

### Má»¥c tiÃªu

Náº¿u user khÃ´ng thanh toÃ¡n trong X phÃºt thÃ¬ order tá»± há»§y vÃ  vÃ© Ä‘Æ°á»£c tráº£ láº¡i.

### Checklist

- [ ] Ä‘áº·t `expiresAt` khi táº¡o order
- [ ] scheduler scan order háº¿t háº¡n
- [ ] Ä‘á»•i tráº¡ng thÃ¡i `EXPIRED` / `CANCELLED`
- [ ] publish `order.expired`
- [ ] inventory-service release láº¡i vÃ©
- [ ] test end-to-end timeout

### Done khi

- order khÃ´ng thanh toÃ¡n sáº½ tá»± háº¿t háº¡n
- inventory Ä‘Æ°á»£c tráº£ láº¡i Ä‘Ãºng

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `TODO`

---

## Phase 14 - Observability cÆ¡ báº£n

### Má»¥c tiÃªu

Biáº¿t há»‡ Ä‘ang sá»‘ng hay cháº¿t, chá»— nÃ o cháº­m, chá»— nÃ o lá»—i.

### Checklist

- [ ] chuáº©n hÃ³a actuator health
- [ ] metrics cho request count / error count
- [ ] prometheus scrape local
- [ ] dashboard cÆ¡ báº£n
- [ ] log correlation id cÆ¡ báº£n

### Done khi

- nhÃ¬n Ä‘Æ°á»£c health/metrics cá»§a tá»«ng service

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `TODO`

---

## Phase 15 - Hardening vÃ  tÃ i liá»‡u

### Má»¥c tiÃªu

Dá»n ná»£ ká»¹ thuáº­t, chuáº©n hÃ³a cÃ¡ch má»Ÿ rá»™ng service tiáº¿p.

### Háº¡ng má»¥c

- Flyway chuáº©n cho má»i service
- service-template Ä‘á»“ng nháº¥t hÆ¡n
- base outbox abstraction dÃ¹ng láº¡i Ä‘Æ°á»£c
- base idempotency abstraction cho API/consumer
- contract test cho event
- replay failed outbox pattern
- docs/adr Ä‘áº§y Ä‘á»§ hÆ¡n

### Checklist

- [ ] chuáº©n hÃ³a template cho service má»›i
- [ ] thÃªm docs ADR cáº§n thiáº¿t
- [ ] ghi runbook local/dev
- [ ] note viá»‡c cáº§n lÃ m tiáº¿p sau phase chÃ­nh
- [ ] review encoding/comment/docs

### Done khi

- project dá»… má»Ÿ rá»™ng, dá»… dáº¡y láº¡i, dá»… tiáº¿p tá»¥c sau nÃ y

### Tráº¡ng thÃ¡i hiá»‡n táº¡i

- `TODO`

---

## 5. Thá»© tá»± Æ°u tiÃªn thá»±c táº¿ tá»« bÃ¢y giá»

### Æ¯u tiÃªn ngay

1. chá»‘t `Phase 6 - Event service`
2. sang `Phase 7 - Inventory service`
3. sang `Phase 8 - Order service`
4. lÃ m `Phase 9 - Idempotency API`
5. lÃ m `Phase 10 - Payment service`
6. lÃ m `Phase 11 - Kafka + Outbox`
7. lÃ m `Phase 12 - Consumer idempotent`
8. lÃ m `Phase 13 - Timeout + release inventory`

### ChÆ°a Æ°u tiÃªn lÃºc nÃ y

- production deploy chuáº©n
- CI/CD
- cloud infra
- Docker secrets
- Kubernetes
- auto refresh config nÃ¢ng cao

---

## 6. CÃ¡ch tá»± theo dÃµi tiáº¿n Ä‘á»™

Má»—i khi xong má»™t phase hoáº·c sub-phase, cáº­p nháº­t 3 thá»©:

1. tráº¡ng thÃ¡i
- `TODO`
- `IN PROGRESS`
- `DONE`

2. báº±ng chá»©ng
- lá»‡nh nÃ o pass
- API nÃ o test pass
- service nÃ o healthy

3. ná»£ ká»¹ thuáº­t cÃ²n láº¡i
- chÆ°a test local non-docker
- chÆ°a thÃªm healthcheck
- chÆ°a thÃªm retry/outbox
- chÆ°a xá»­ lÃ½ idempotency

---

## 7. Tráº¡ng thÃ¡i hiá»‡n táº¡i toÃ n dá»± Ã¡n

### ÄÃ£ xong

- [x] Phase 1
- [x] Phase 1.5
- [x] Phase 2
- [x] Phase 3
- [x] Phase 4
- [x] Phase 5
- [x] Phase 6

### Äang lÃ m


### ChÆ°a lÃ m

- [ ] Phase 7
- [ ] Phase 8
- [ ] Phase 9
- [ ] Phase 10
- [ ] Phase 11
- [ ] Phase 12
- [ ] Phase 13
- [ ] Phase 14
- [ ] Phase 15

---

## 8. BÆ°á»›c tiáº¿p theo ngay bÃ¢y giá»

1. báº¯t Ä‘áº§u dá»±ng `inventory-service`
2. táº¡o báº£ng tá»“n kho vÃ© theo `eventId`
3. táº¡o API create/get inventory
4. ná»‘i gateway + Docker local
