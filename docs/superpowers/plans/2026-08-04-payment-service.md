# Payment Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Xây `payment-service` mô phỏng thanh toán, sau đó cập nhật `order-service` sang `CONFIRMED` hoặc `CANCELLED` bằng HTTP đồng bộ.

**Architecture:** Phase 10.1 tạo `payment-service` độc lập với database riêng và ba trạng thái `PENDING`, `COMPLETED`, `FAILED`. Phase 10.2 thêm `OrderClient`; payment-service kiểm tra order trước khi tạo payment và gọi API nội bộ của order-service trước khi lưu trạng thái cuối của payment. Phase 11 mới thay HTTP đồng bộ bằng Kafka và Outbox.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring Cloud 2023.0.3, Spring MVC, RestClient, Spring Data JPA, Flyway, MySQL 8.4, Eureka, Config Server, Gateway, Docker Compose, JUnit 5, Mockito.

## Global Constraints

- Dùng package `com.ticketsale.payment` cho module mới.
- Dùng port `8095` cho cả local và Docker local.
- Dùng database riêng `payment_service_db`.
- Dùng `spring.jpa.hibernate.ddl-auto: validate`; Flyway sở hữu schema.
- Dùng `ApiResponse.ok(...)` và `ApiResponse.fail(...)` từ module `common`.
- Comment code và SQL bằng tiếng Việt; file UTF-8 không BOM.
- Không thêm `amount` vì project chưa có domain giá vé.
- Không thêm Kafka, Outbox, retry, circuit breaker, auth, refund hoặc payment provider thật trong Phase 10.
- Payment-service không được đọc hoặc ghi trực tiếp database của order-service.
- Một `orderNo` chỉ có một payment record; unique constraint bảo vệ quy tắc này.
- Transition hợp lệ: `PENDING -> COMPLETED` hoặc `PENDING -> FAILED`.
- Gọi lại cùng transition trả record hiện tại; transition ngược trả HTTP `409`.
- Khi complete/fail, gọi order-service trước rồi mới lưu trạng thái cuối của payment.
- Không commit hoặc push tự động; chỉ chạy `git diff` tại checkpoint, chờ người dùng yêu cầu Git.

---

## File Structure

### Tạo mới trong payment-service

- `payment-service/pom.xml`: dependency và build plugin của module.
- `payment-service/Dockerfile`: chạy JAR bằng Java 21 JRE.
- `payment-service/src/main/java/com/ticketsale/payment/PaymentServiceApplication.java`: Spring Boot entry point.
- `payment-service/src/main/resources/application.yml`: tên service, Config Server, port và actuator.
- `payment-service/src/main/resources/db/migration/V1__create_payments_table.sql`: schema bảng `payments`.
- `payment-service/src/main/java/com/ticketsale/payment/repository/entity/PaymentStatus.java`: ba trạng thái payment.
- `payment-service/src/main/java/com/ticketsale/payment/repository/entity/PaymentEntity.java`: ánh xạ bảng và đổi trạng thái payment.
- `payment-service/src/main/java/com/ticketsale/payment/repository/PaymentRepository.java`: tìm payment theo `paymentNo` hoặc `orderNo`.
- `payment-service/src/main/java/com/ticketsale/payment/client/dto/OrderCheckoutResponse.java`: dữ liệu checkout nhận từ order-service.
- `payment-service/src/main/java/com/ticketsale/payment/client/OrderClient.java`: gọi checkout và API nội bộ order.
- `payment-service/src/main/java/com/ticketsale/payment/controller/dto/request/CreatePaymentRequest.java`: body tạo payment.
- `payment-service/src/main/java/com/ticketsale/payment/controller/dto/request/FailPaymentRequest.java`: body payment thất bại.
- `payment-service/src/main/java/com/ticketsale/payment/controller/dto/response/PaymentResponse.java`: response public của payment.
- `payment-service/src/main/java/com/ticketsale/payment/service/PaymentService.java`: interface nghiệp vụ.
- `payment-service/src/main/java/com/ticketsale/payment/service/impl/PaymentServiceImpl.java`: create, complete và fail.
- `payment-service/src/main/java/com/ticketsale/payment/controller/PaymentController.java`: ba API public.
- `payment-service/src/main/java/com/ticketsale/payment/exception/PaymentNotFoundException.java`: lỗi payment không tồn tại.
- `payment-service/src/main/java/com/ticketsale/payment/exception/PaymentConflictException.java`: lỗi transition hoặc trạng thái order không hợp lệ.
- `payment-service/src/main/java/com/ticketsale/payment/exception/OrderNotFoundException.java`: order-service trả `404`.
- `payment-service/src/main/java/com/ticketsale/payment/exception/OrderServiceUnavailableException.java`: lỗi mạng hoặc lỗi upstream ngoài nghiệp vụ.
- `payment-service/src/main/java/com/ticketsale/payment/exception/GlobalExceptionHandler.java`: map lỗi thành `400`, `404`, `409`, `502`.

### Tạo mới trong test

- `payment-service/src/test/java/com/ticketsale/payment/repository/entity/PaymentEntityTest.java`: kiểm tra trạng thái entity.
- `payment-service/src/test/java/com/ticketsale/payment/client/OrderClientTest.java`: kiểm tra URL, method và mapping lỗi HTTP.
- `payment-service/src/test/java/com/ticketsale/payment/service/impl/PaymentServiceImplTest.java`: kiểm tra logic create/complete/fail.
- `payment-service/src/test/java/com/ticketsale/payment/controller/PaymentControllerTest.java`: kiểm tra contract HTTP.
- `order-service/src/test/java/com/ticketsale/order/controller/InternalOrderControllerTest.java`: kiểm tra API nội bộ.

### Sửa file hiện có

- `pom.xml`: khai báo module `payment-service`.
- `order-service/src/main/java/com/ticketsale/order/repository/entity/OrderEntity.java`: thêm hàm confirm/cancel.
- `order-service/src/main/java/com/ticketsale/order/service/OrderService.java`: thêm hai operation payment.
- `order-service/src/main/java/com/ticketsale/order/service/impl/OrderServiceImpl.java`: kiểm tra và đổi trạng thái order.
- `order-service/src/main/java/com/ticketsale/order/exception/GlobalExceptionHandler.java`: map `404` và `409`.
- `order-service/src/test/java/com/ticketsale/order/service/impl/OrderServiceImplTest.java`: test transition order.
- `environment/config-repo/gateway-dev.yml`: route payment local.
- `environment/config-repo/gateway-docker.yml`: route payment Docker.
- `docker-compose.yml`: build, healthcheck và dependency của payment-service.
- `.env.example`, `.env.dev.example`, `.env.prod.example`: khai báo `PAYMENT_SERVICE_PORT=8095`.
- `docs/ke-hoach-cong-viec.md`: cập nhật checklist và bằng chứng Phase 10 sau khi verify.

---

### Task 1: Tạo module payment-service chạy được

**Files:**
- Create: `payment-service/pom.xml`
- Create: `payment-service/Dockerfile`
- Create: `payment-service/src/main/java/com/ticketsale/payment/PaymentServiceApplication.java`
- Modify: `pom.xml`

**Interfaces:**
- Consumes: parent Maven `com.ticketsale:ticket-sale-manual:1.0-SNAPSHOT`.
- Produces: executable module `com.ticketsale:payment-service:1.0-SNAPSHOT`.

- [ ] **Step 1: Thêm module vào root POM**

Trong `pom.xml`, thêm sau `order-service`:

```xml
<module>payment-service</module>
```

- [ ] **Step 2: Tạo payment-service POM**

Tạo `payment-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ticketsale</groupId>
        <artifactId>ticket-sale-manual</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>payment-service</artifactId>
    <name>payment-service</name>

    <dependencies>
        <dependency>
            <groupId>com.ticketsale</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Tạo Spring Boot entry point**

Tạo `PaymentServiceApplication.java`:

```java
package com.ticketsale.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
```

- [ ] **Step 4: Tạo Dockerfile**

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/payment-service-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- [ ] **Step 5: Compile module**

Run:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl payment-service -am clean package -DskipTests
```

Expected: `BUILD SUCCESS` và có `payment-service/target/payment-service-1.0-SNAPSHOT.jar`.

- [ ] **Step 6: Review checkpoint**

```powershell
git diff -- pom.xml payment-service
```

Expected: chỉ có module mới và khai báo module root.

---

### Task 2: Tạo schema và persistence model

**Files:**
- Create: `payment-service/src/main/resources/db/migration/V1__create_payments_table.sql`
- Create: `payment-service/src/main/java/com/ticketsale/payment/repository/entity/PaymentStatus.java`
- Create: `payment-service/src/main/java/com/ticketsale/payment/repository/entity/PaymentEntity.java`
- Create: `payment-service/src/main/java/com/ticketsale/payment/repository/PaymentRepository.java`
- Test: `payment-service/src/test/java/com/ticketsale/payment/repository/entity/PaymentEntityTest.java`

**Interfaces:**
- Produces: `PaymentStatus`, `PaymentEntity`, `PaymentRepository.findByPaymentNo(String)`, `PaymentRepository.findByOrderNo(String)`.

- [ ] **Step 1: Viết test entity trước**

Tạo `PaymentEntityTest.java` với ba test:

```java
package com.ticketsale.payment.repository.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentEntityTest {

    @Test
    void newPaymentShouldStartPending() {
        PaymentEntity payment = new PaymentEntity("PAY-123", "ORD-123");

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNull(payment.getFailureReason());
    }

    @Test
    void completeShouldMarkPaymentCompleted() {
        PaymentEntity payment = new PaymentEntity("PAY-123", "ORD-123");

        payment.complete();

        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertNull(payment.getFailureReason());
    }

    @Test
    void failShouldStoreFailureReason() {
        PaymentEntity payment = new PaymentEntity("PAY-123", "ORD-123");

        payment.fail("PAYMENT_DECLINED");

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("PAYMENT_DECLINED", payment.getFailureReason());
    }
}
```

- [ ] **Step 2: Chạy test để thấy RED**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl payment-service -Dtest=PaymentEntityTest test
```

Expected: compile fail vì `PaymentEntity` và `PaymentStatus` chưa tồn tại.

- [ ] **Step 3: Tạo migration**

Tạo `V1__create_payments_table.sql`:

```sql
-- Mỗi order chỉ có một payment record trong Payment v1.
create table payments
(
    id             bigint primary key auto_increment,
    payment_no     varchar(100) not null,
    order_no       varchar(100) not null,
    status         varchar(50)  not null,
    failure_reason varchar(255) null,
    created_at     datetime     not null,
    updated_at     datetime     not null,

    -- Mã payment dùng cho API public, không dùng ID database.
    constraint uk_payments_payment_no unique (payment_no),

    -- Request tạo payment lặp lại cho cùng order phải dùng lại record cũ.
    constraint uk_payments_order_no unique (order_no)
);
```

- [ ] **Step 4: Tạo enum và entity**

`PaymentStatus.java`:

```java
package com.ticketsale.payment.repository.entity;

public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED
}
```

`PaymentEntity.java` dùng đầy đủ nội dung sau:

```java
package com.ticketsale.payment.repository.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_no", nullable = false, unique = true, length = 100)
    private String paymentNo;

    @Column(name = "order_no", nullable = false, unique = true, length = 100)
    private String orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected PaymentEntity() {
    }

    public PaymentEntity(String paymentNo, String orderNo) {
        this.paymentNo = paymentNo;
        this.orderNo = orderNo;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Đánh dấu thanh toán thành công sau khi order-service đã confirm order.
    public void complete() {
        this.status = PaymentStatus.COMPLETED;
        this.failureReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    // Lưu trạng thái và lý do thất bại sau khi order-service đã cancel order.
    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
```

- [ ] **Step 5: Tạo repository**

```java
package com.ticketsale.payment.repository;

import com.ticketsale.payment.repository.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByPaymentNo(String paymentNo);

    Optional<PaymentEntity> findByOrderNo(String orderNo);
}
```

- [ ] **Step 6: Chạy test để thấy GREEN**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl payment-service -Dtest=PaymentEntityTest test
```

Expected: `3 tests`, `BUILD SUCCESS`.

---

### Task 3: Thêm API nội bộ đổi trạng thái order

**Files:**
- Create: `order-service/src/main/java/com/ticketsale/order/controller/InternalOrderController.java`
- Create: `order-service/src/main/java/com/ticketsale/order/exception/OrderNotFoundException.java`
- Create: `order-service/src/main/java/com/ticketsale/order/exception/OrderConflictException.java`
- Modify: `order-service/src/main/java/com/ticketsale/order/repository/entity/OrderEntity.java`
- Modify: `order-service/src/main/java/com/ticketsale/order/service/OrderService.java`
- Modify: `order-service/src/main/java/com/ticketsale/order/service/impl/OrderServiceImpl.java`
- Modify: `order-service/src/main/java/com/ticketsale/order/exception/GlobalExceptionHandler.java`
- Test: `order-service/src/test/java/com/ticketsale/order/service/impl/OrderServiceImplTest.java`
- Test: `order-service/src/test/java/com/ticketsale/order/controller/InternalOrderControllerTest.java`

**Interfaces:**
- Produces: `OrderService.markPaymentCompleted(String)` và `OrderService.markPaymentFailed(String)`.
- Produces: `POST /internal/orders/{orderNo}/payment-completed` và `POST /internal/orders/{orderNo}/payment-failed`.

- [ ] **Step 1: Thêm failing service tests**

Thêm vào `OrderServiceImplTest.java`:

```java
@Test
void markPaymentCompletedShouldConfirmPendingOrder() {
    OrderEntity order = new OrderEntity(
            "ORD-123", 1L, 1001L, 2, LocalDateTime.now().plusMinutes(10)
    );
    when(orderRepository.findByOrderNo("ORD-123")).thenReturn(Optional.of(order));
    when(orderRepository.save(order)).thenReturn(order);

    OrderResponse response = orderService.markPaymentCompleted("ORD-123");

    assertEquals(OrderStatus.CONFIRMED, response.status());
}

@Test
void markPaymentCompletedShouldReturnConfirmedOrderWithoutChangingAgain() {
    OrderEntity order = new OrderEntity(
            "ORD-123", 1L, 1001L, 2, LocalDateTime.now().plusMinutes(10)
    );
    order.confirm();
    when(orderRepository.findByOrderNo("ORD-123")).thenReturn(Optional.of(order));

    OrderResponse response = orderService.markPaymentCompleted("ORD-123");

    assertEquals(OrderStatus.CONFIRMED, response.status());
    verify(orderRepository, never()).save(order);
}

@Test
void markPaymentFailedShouldCancelPendingOrder() {
    OrderEntity order = new OrderEntity(
            "ORD-456", 1L, 1002L, 1, LocalDateTime.now().plusMinutes(10)
    );
    when(orderRepository.findByOrderNo("ORD-456")).thenReturn(Optional.of(order));
    when(orderRepository.save(order)).thenReturn(order);

    OrderResponse response = orderService.markPaymentFailed("ORD-456");

    assertEquals(OrderStatus.CANCELLED, response.status());
}

@Test
void markPaymentFailedShouldRejectConfirmedOrder() {
    OrderEntity order = new OrderEntity(
            "ORD-789", 1L, 1003L, 1, LocalDateTime.now().plusMinutes(10)
    );
    order.confirm();
    when(orderRepository.findByOrderNo("ORD-789")).thenReturn(Optional.of(order));

    assertThrows(
            OrderConflictException.class,
            () -> orderService.markPaymentFailed("ORD-789")
    );
}
```

- [ ] **Step 2: Chạy service tests để thấy RED**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl order-service -Dtest=OrderServiceImplTest test
```

Expected: compile fail vì method và exception chưa tồn tại.

- [ ] **Step 3: Thêm mutation vào OrderEntity**

```java
// Xác nhận order sau khi payment thành công.
public void confirm() {
    this.status = OrderStatus.CONFIRMED;
    this.updatedAt = LocalDateTime.now();
}

// Hủy order sau khi payment thất bại.
public void cancel() {
    this.status = OrderStatus.CANCELLED;
    this.updatedAt = LocalDateTime.now();
}
```

- [ ] **Step 4: Thêm interface và logic service**

Thêm vào `OrderService.java`:

```java
OrderResponse markPaymentCompleted(String orderNo);

OrderResponse markPaymentFailed(String orderNo);
```

Thêm vào `OrderServiceImpl.java`:

```java
@Override
@Transactional
public OrderResponse markPaymentCompleted(String orderNo) {
    OrderEntity entity = findByOrderNo(orderNo);

    if (entity.getStatus() == OrderStatus.CONFIRMED) {
        return toResponse(entity);
    }
    if (entity.getStatus() != OrderStatus.PENDING_PAYMENT) {
        throw new OrderConflictException("Order không thể chuyển sang CONFIRMED");
    }
    if (!LocalDateTime.now().isBefore(entity.getExpiresAt())) {
        throw new OrderConflictException("Order đã hết hạn thanh toán");
    }

    entity.confirm();
    return toResponse(orderRepository.save(entity));
}

@Override
@Transactional
public OrderResponse markPaymentFailed(String orderNo) {
    OrderEntity entity = findByOrderNo(orderNo);

    if (entity.getStatus() == OrderStatus.CANCELLED
            || entity.getStatus() == OrderStatus.EXPIRED) {
        return toResponse(entity);
    }
    if (entity.getStatus() == OrderStatus.CONFIRMED) {
        throw new OrderConflictException("Order đã CONFIRMED nên không thể hủy");
    }

    entity.cancel();
    return toResponse(orderRepository.save(entity));
}
```

Đổi `findByOrderNo(...)` sang ném `OrderNotFoundException("Không tìm thấy order")` thay vì `IllegalArgumentException`.

- [ ] **Step 5: Thêm exception mapping**

Trong `GlobalExceptionHandler.java`:

```java
@ExceptionHandler(OrderNotFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public ApiResponse<Void> handleNotFound(OrderNotFoundException exception) {
    return ApiResponse.fail(exception.getMessage());
}

@ExceptionHandler(OrderConflictException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public ApiResponse<Void> handleConflict(OrderConflictException exception) {
    return ApiResponse.fail(exception.getMessage());
}
```

- [ ] **Step 6: Tạo InternalOrderController**

```java
package com.ticketsale.order.controller;

import com.ticketsale.common.response.ApiResponse;
import com.ticketsale.order.controller.dto.response.OrderResponse;
import com.ticketsale.order.service.OrderService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/orders")
public class InternalOrderController {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Payment-service gọi endpoint này khi thanh toán thành công.
    @PostMapping("/{orderNo}/payment-completed")
    public ApiResponse<OrderResponse> paymentCompleted(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.markPaymentCompleted(orderNo));
    }

    // Payment-service gọi endpoint này khi thanh toán thất bại.
    @PostMapping("/{orderNo}/payment-failed")
    public ApiResponse<OrderResponse> paymentFailed(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.markPaymentFailed(orderNo));
    }
}
```

- [ ] **Step 7: Thêm controller tests**

`InternalOrderControllerTest.java` dùng `@WebMvcTest(value = InternalOrderController.class, properties = "spring.cloud.config.enabled=false")` và kiểm tra:

```java
mockMvc.perform(post("/internal/orders/ORD-123/payment-completed"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

mockMvc.perform(post("/internal/orders/ORD-456/payment-failed"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CANCELLED"));
```

- [ ] **Step 8: Chạy order-service tests**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl order-service clean test
```

Expected: toàn bộ order-service tests pass.

---

### Task 4: Tạo OrderClient trong payment-service

**Files:**
- Create: `payment-service/src/main/java/com/ticketsale/payment/client/dto/OrderCheckoutResponse.java`
- Create: `payment-service/src/main/java/com/ticketsale/payment/client/OrderClient.java`
- Create: bốn exception trong `payment-service/src/main/java/com/ticketsale/payment/exception/`
- Test: `payment-service/src/test/java/com/ticketsale/payment/client/OrderClientTest.java`

**Interfaces:**
- Consumes: `GET /api/orders/{orderNo}/checkout`.
- Consumes: `POST /internal/orders/{orderNo}/payment-completed`.
- Consumes: `POST /internal/orders/{orderNo}/payment-failed`.
- Produces: `OrderCheckoutResponse getCheckout(String orderNo)`, `void markPaymentCompleted(String orderNo)`, `void markPaymentFailed(String orderNo)`.

- [ ] **Step 1: Tạo client DTO**

```java
package com.ticketsale.payment.client.dto;

import java.time.LocalDateTime;

public record OrderCheckoutResponse(
        String orderNo,
        String status,
        LocalDateTime expiresAt
) {
}
```

Không import `OrderStatus` từ order-service. Hai service chỉ chia sẻ JSON contract, không phụ thuộc Java module của nhau.

- [ ] **Step 2: Viết failing client tests**

Trong `OrderClientTest.java`, dùng `MockRestServiceServer` giống `InventoryClientTest` và kiểm tra ba case:

```java
server.expect(requestTo("http://order-service:8094/api/orders/ORD-123/checkout"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("""
                {
                  "success": true,
                  "message": "OK",
                  "data": {
                    "orderNo": "ORD-123",
                    "status": "PENDING_PAYMENT",
                    "expiresAt": "2026-08-04T22:00:00"
                  },
                  "timestamp": "2026-08-04T21:50:00"
                }
                """, MediaType.APPLICATION_JSON));

OrderCheckoutResponse response = orderClient.getCheckout("ORD-123");
assertEquals("PENDING_PAYMENT", response.status());
```

```java
server.expect(requestTo("http://order-service:8094/internal/orders/ORD-123/payment-completed"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

orderClient.markPaymentCompleted("ORD-123");
```

```java
server.expect(requestTo("http://order-service:8094/api/orders/MISSING/checkout"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"success\":false,\"message\":\"Không tìm thấy order\",\"data\":null}"));

assertThrows(OrderNotFoundException.class, () -> orderClient.getCheckout("MISSING"));
```

- [ ] **Step 3: Chạy client tests để thấy RED**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl payment-service -Dtest=OrderClientTest test
```

- [ ] **Step 4: Tạo exception đơn giản**

Tạo bốn file exception. Mỗi file dùng đúng một class tương ứng.

```java
public class PaymentConflictException extends RuntimeException {
    public PaymentConflictException(String message) {
        super(message);
    }
}
```

`PaymentNotFoundException` và `OrderNotFoundException` dùng cùng constructor một tham số. `OrderServiceUnavailableException` có cả hai constructor:

```java
public class OrderServiceUnavailableException extends RuntimeException {

    public OrderServiceUnavailableException(String message) {
        super(message);
    }

    public OrderServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 5: Tạo OrderClient**

Constructor:

```java
public OrderClient(
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        @Value("${order.service.base-url}") String orderBaseUrl
) {
    this.restClient = restClientBuilder.baseUrl(orderBaseUrl).build();
    this.objectMapper = objectMapper;
}
```

Methods:

```java
public OrderCheckoutResponse getCheckout(String orderNo) {
    try {
        ApiResponse<OrderCheckoutResponse> response = restClient.get()
                .uri("/api/orders/{orderNo}/checkout", orderNo)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || response.data() == null) {
            throw new OrderServiceUnavailableException("order-service trả response rỗng");
        }
        return response.data();
    } catch (RestClientResponseException exception) {
        throw mapHttpError(exception);
    } catch (RestClientException exception) {
        throw new OrderServiceUnavailableException("Không gọi được order-service", exception);
    }
}

public void markPaymentCompleted(String orderNo) {
    postTransition("/internal/orders/{orderNo}/payment-completed", orderNo);
}

public void markPaymentFailed(String orderNo) {
    postTransition("/internal/orders/{orderNo}/payment-failed", orderNo);
}

private void postTransition(String uriTemplate, String orderNo) {
    try {
        restClient.post()
                .uri(uriTemplate, orderNo)
                .retrieve()
                .toBodilessEntity();
    } catch (RestClientResponseException exception) {
        throw mapHttpError(exception);
    } catch (RestClientException exception) {
        throw new OrderServiceUnavailableException("Không gọi được order-service", exception);
    }
}

private RuntimeException mapHttpError(RestClientResponseException exception) {
    return switch (exception.getStatusCode().value()) {
        case 404 -> new OrderNotFoundException(extractMessage(exception));
        case 409 -> new PaymentConflictException(extractMessage(exception));
        default -> new OrderServiceUnavailableException(
                "order-service trả lỗi: " + extractMessage(exception),
                exception
        );
    };
}

private String extractMessage(RestClientResponseException exception) {
    try {
        JsonNode root = objectMapper.readTree(exception.getResponseBodyAsString());
        String message = root.path("message").asText();
        if (!message.isBlank()) {
            return message;
        }
    } catch (Exception ignored) {
        // Body lỗi không phải JSON thì dùng message gốc của RestClient.
    }
    return exception.getMessage();
}
```

`extractMessage(...)` dùng `ObjectMapper` được Spring inject, không `new ObjectMapper()`.

- [ ] **Step 6: Chạy client tests để thấy GREEN**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl payment-service -Dtest=OrderClientTest test
```

Expected: client tests pass.

---

### Task 5: Tạo payment PENDING

**Files:**
- Create: `payment-service/src/main/java/com/ticketsale/payment/controller/dto/request/CreatePaymentRequest.java`
- Create: `payment-service/src/main/java/com/ticketsale/payment/controller/dto/request/FailPaymentRequest.java`
- Create: `payment-service/src/main/java/com/ticketsale/payment/controller/dto/response/PaymentResponse.java`
- Create: `payment-service/src/main/java/com/ticketsale/payment/service/PaymentService.java`
- Create: `payment-service/src/main/java/com/ticketsale/payment/service/impl/PaymentServiceImpl.java`
- Test: `payment-service/src/test/java/com/ticketsale/payment/service/impl/PaymentServiceImplTest.java`

**Interfaces:**
- Consumes: `OrderClient.getCheckout(String)`.
- Produces: `PaymentResponse create(CreatePaymentRequest request)`.

- [ ] **Step 1: Tạo DTO exact**

```java
public record CreatePaymentRequest(
        @NotBlank(message = "orderNo không được để trống") String orderNo
) {
}
```

```java
public record PaymentResponse(
        Long id,
        String paymentNo,
        String orderNo,
        PaymentStatus status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
```

- [ ] **Step 2: Viết failing service tests cho create**

```java
@Test
void createShouldCreatePendingPaymentForPayableOrder() {
    when(paymentRepository.findByOrderNo("ORD-123")).thenReturn(Optional.empty());
    when(orderClient.getCheckout("ORD-123")).thenReturn(new OrderCheckoutResponse(
            "ORD-123", "PENDING_PAYMENT", LocalDateTime.now().plusMinutes(10)
    ));
    when(paymentRepository.save(any(PaymentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    PaymentResponse response = paymentService.create(new CreatePaymentRequest("ORD-123"));

    assertEquals("ORD-123", response.orderNo());
    assertEquals(PaymentStatus.PENDING, response.status());
    assertNotNull(response.paymentNo());
}

@Test
void createShouldReturnExistingPaymentWithoutCallingOrderService() {
    PaymentEntity existing = new PaymentEntity("PAY-OLD", "ORD-123");
    when(paymentRepository.findByOrderNo("ORD-123")).thenReturn(Optional.of(existing));

    PaymentResponse response = paymentService.create(new CreatePaymentRequest("ORD-123"));

    assertEquals("PAY-OLD", response.paymentNo());
    verify(orderClient, never()).getCheckout(any());
    verify(paymentRepository, never()).save(any());
}

@Test
void createShouldRejectOrderThatIsNotPendingPayment() {
    when(paymentRepository.findByOrderNo("ORD-123")).thenReturn(Optional.empty());
    when(orderClient.getCheckout("ORD-123")).thenReturn(new OrderCheckoutResponse(
            "ORD-123", "CONFIRMED", LocalDateTime.now().plusMinutes(10)
    ));

    assertThrows(
            PaymentConflictException.class,
            () -> paymentService.create(new CreatePaymentRequest("ORD-123"))
    );
}

@Test
void createShouldRejectExpiredOrder() {
    when(paymentRepository.findByOrderNo("ORD-123")).thenReturn(Optional.empty());
    when(orderClient.getCheckout("ORD-123")).thenReturn(new OrderCheckoutResponse(
            "ORD-123", "PENDING_PAYMENT", LocalDateTime.now().minusSeconds(1)
    ));

    assertThrows(
            PaymentConflictException.class,
            () -> paymentService.create(new CreatePaymentRequest("ORD-123"))
    );
}
```

- [ ] **Step 3: Chạy tests để thấy RED**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl payment-service -Dtest=PaymentServiceImplTest test
```

- [ ] **Step 4: Tạo PaymentService interface**

```java
public interface PaymentService {

    PaymentResponse create(CreatePaymentRequest request);

    PaymentResponse complete(String paymentNo);

    PaymentResponse fail(String paymentNo, FailPaymentRequest request);
}
```

Tạo `FailPaymentRequest.java` ngay trong Task 5 để interface và controller các task sau dùng cùng type:

```java
public record FailPaymentRequest(
        @NotBlank(message = "reason không được để trống") String reason
) {
}
```

- [ ] **Step 5: Implement create tối thiểu**

Tạo `PaymentServiceImpl` với hai dependency:

```java
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderClient orderClient
    ) {
        this.paymentRepository = paymentRepository;
        this.orderClient = orderClient;
    }
}
```

```java
@Override
@Transactional
public PaymentResponse create(CreatePaymentRequest request) {
    PaymentEntity existing = paymentRepository.findByOrderNo(request.orderNo())
            .orElse(null);
    if (existing != null) {
        return toResponse(existing);
    }

    OrderCheckoutResponse checkout = orderClient.getCheckout(request.orderNo());
    if (!"PENDING_PAYMENT".equals(checkout.status())) {
        throw new PaymentConflictException("Order không ở trạng thái chờ thanh toán");
    }
    if (!LocalDateTime.now().isBefore(checkout.expiresAt())) {
        throw new PaymentConflictException("Order đã hết hạn thanh toán");
    }

    PaymentEntity payment = new PaymentEntity(
            "PAY-" + UUID.randomUUID(),
            request.orderNo()
    );
    return toResponse(paymentRepository.save(payment));
}

private PaymentEntity findByPaymentNo(String paymentNo) {
    return paymentRepository.findByPaymentNo(paymentNo)
            .orElseThrow(() -> new PaymentNotFoundException("Không tìm thấy payment"));
}

private PaymentResponse toResponse(PaymentEntity entity) {
    return new PaymentResponse(
            entity.getId(),
            entity.getPaymentNo(),
            entity.getOrderNo(),
            entity.getStatus(),
            entity.getFailureReason(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
    );
}
```

- [ ] **Step 6: Chạy tests để thấy GREEN**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl payment-service -Dtest=PaymentServiceImplTest test
```

---

### Task 6: Hoàn thiện complete/fail idempotent

**Files:**
- Modify: `payment-service/src/main/java/com/ticketsale/payment/service/impl/PaymentServiceImpl.java`
- Test: `payment-service/src/test/java/com/ticketsale/payment/service/impl/PaymentServiceImplTest.java`

**Interfaces:**
- Consumes: `OrderClient.markPaymentCompleted(String)` và `OrderClient.markPaymentFailed(String)`.
- Produces: `PaymentService.complete(String)` và `PaymentService.fail(String, FailPaymentRequest)`.

- [ ] **Step 1: Viết failing tests cho complete**

```java
@Test
void completeShouldUpdateOrderBeforeSavingPayment() {
    PaymentEntity payment = new PaymentEntity("PAY-123", "ORD-123");
    when(paymentRepository.findByPaymentNo("PAY-123")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(payment)).thenReturn(payment);

    PaymentResponse response = paymentService.complete("PAY-123");

    InOrder calls = inOrder(orderClient, paymentRepository);
    calls.verify(orderClient).markPaymentCompleted("ORD-123");
    calls.verify(paymentRepository).save(payment);
    assertEquals(PaymentStatus.COMPLETED, response.status());
}

@Test
void completeShouldReturnCompletedPaymentWithoutCallingOrderAgain() {
    PaymentEntity payment = new PaymentEntity("PAY-123", "ORD-123");
    payment.complete();
    when(paymentRepository.findByPaymentNo("PAY-123")).thenReturn(Optional.of(payment));

    PaymentResponse response = paymentService.complete("PAY-123");

    assertEquals(PaymentStatus.COMPLETED, response.status());
    verify(orderClient, never()).markPaymentCompleted(any());
}

@Test
void completeShouldRejectFailedPayment() {
    PaymentEntity payment = new PaymentEntity("PAY-123", "ORD-123");
    payment.fail("PAYMENT_DECLINED");
    when(paymentRepository.findByPaymentNo("PAY-123")).thenReturn(Optional.of(payment));

    assertThrows(PaymentConflictException.class, () -> paymentService.complete("PAY-123"));
}
```

- [ ] **Step 2: Viết failing tests cho fail**

```java
@Test
void failShouldUpdateOrderBeforeSavingPayment() {
    PaymentEntity payment = new PaymentEntity("PAY-456", "ORD-456");
    when(paymentRepository.findByPaymentNo("PAY-456")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(payment)).thenReturn(payment);

    PaymentResponse response = paymentService.fail(
            "PAY-456",
            new FailPaymentRequest("PAYMENT_DECLINED")
    );

    InOrder calls = inOrder(orderClient, paymentRepository);
    calls.verify(orderClient).markPaymentFailed("ORD-456");
    calls.verify(paymentRepository).save(payment);
    assertEquals(PaymentStatus.FAILED, response.status());
    assertEquals("PAYMENT_DECLINED", response.failureReason());
}

@Test
void failShouldReturnFailedPaymentWithoutCallingOrderAgain() {
    PaymentEntity payment = new PaymentEntity("PAY-456", "ORD-456");
    payment.fail("PAYMENT_DECLINED");
    when(paymentRepository.findByPaymentNo("PAY-456")).thenReturn(Optional.of(payment));

    PaymentResponse response = paymentService.fail(
            "PAY-456",
            new FailPaymentRequest("PAYMENT_DECLINED")
    );

    assertEquals(PaymentStatus.FAILED, response.status());
    verify(orderClient, never()).markPaymentFailed(any());
}

@Test
void failShouldRejectCompletedPayment() {
    PaymentEntity payment = new PaymentEntity("PAY-456", "ORD-456");
    payment.complete();
    when(paymentRepository.findByPaymentNo("PAY-456")).thenReturn(Optional.of(payment));

    assertThrows(
            PaymentConflictException.class,
            () -> paymentService.fail("PAY-456", new FailPaymentRequest("PAYMENT_DECLINED"))
    );
}
```

- [ ] **Step 3: Implement complete**

```java
@Override
@Transactional
public PaymentResponse complete(String paymentNo) {
    PaymentEntity payment = findByPaymentNo(paymentNo);

    if (payment.getStatus() == PaymentStatus.COMPLETED) {
        return toResponse(payment);
    }
    if (payment.getStatus() == PaymentStatus.FAILED) {
        throw new PaymentConflictException("Payment FAILED không thể chuyển sang COMPLETED");
    }

    orderClient.markPaymentCompleted(payment.getOrderNo());
    payment.complete();
    return toResponse(paymentRepository.save(payment));
}
```

- [ ] **Step 4: Implement fail**

```java
@Override
@Transactional
public PaymentResponse fail(String paymentNo, FailPaymentRequest request) {
    PaymentEntity payment = findByPaymentNo(paymentNo);

    if (payment.getStatus() == PaymentStatus.FAILED) {
        return toResponse(payment);
    }
    if (payment.getStatus() == PaymentStatus.COMPLETED) {
        throw new PaymentConflictException("Payment COMPLETED không thể chuyển sang FAILED");
    }

    orderClient.markPaymentFailed(payment.getOrderNo());
    payment.fail(request.reason());
    return toResponse(paymentRepository.save(payment));
}
```

`findByPaymentNo(...)` phải ném:

```java
throw new PaymentNotFoundException("Không tìm thấy payment");
```

- [ ] **Step 5: Chạy service tests**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl payment-service -Dtest=PaymentServiceImplTest test
```

Expected: create, complete, fail và idempotent tests đều pass.

---

### Task 7: Expose Payment API và chuẩn hóa HTTP errors

**Files:**
- Create: `payment-service/src/main/java/com/ticketsale/payment/controller/PaymentController.java`
- Create: `payment-service/src/main/java/com/ticketsale/payment/exception/GlobalExceptionHandler.java`
- Test: `payment-service/src/test/java/com/ticketsale/payment/controller/PaymentControllerTest.java`

**Interfaces:**
- Produces: `POST /api/payments`.
- Produces: `POST /api/payments/{paymentNo}/complete`.
- Produces: `POST /api/payments/{paymentNo}/fail`.

- [ ] **Step 1: Viết controller tests**

`PaymentControllerTest.java` dùng:

```java
@WebMvcTest(value = PaymentController.class, properties = "spring.cloud.config.enabled=false")
```

Kiểm tra create:

```java
mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new CreatePaymentRequest("ORD-123")
                )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.paymentNo").value("PAY-123"))
        .andExpect(jsonPath("$.data.status").value("PENDING"));
```

Kiểm tra complete:

```java
mockMvc.perform(post("/api/payments/PAY-123/complete"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPLETED"));
```

Kiểm tra fail:

```java
mockMvc.perform(post("/api/payments/PAY-456/fail")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"PAYMENT_DECLINED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("FAILED"));
```

Kiểm tra validation và conflict:

```java
mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNo\":\"\"}"))
        .andExpect(status().isBadRequest());

when(paymentService.complete("PAY-FAILED"))
        .thenThrow(new PaymentConflictException("Payment FAILED không thể chuyển sang COMPLETED"));

mockMvc.perform(post("/api/payments/PAY-FAILED/complete"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false));
```

- [ ] **Step 2: Chạy controller tests để thấy RED**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl payment-service -Dtest=PaymentControllerTest test
```

- [ ] **Step 3: Tạo PaymentController**

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ApiResponse<PaymentResponse> create(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        return ApiResponse.ok(paymentService.create(request));
    }

    @PostMapping("/{paymentNo}/complete")
    public ApiResponse<PaymentResponse> complete(@PathVariable String paymentNo) {
        return ApiResponse.ok(paymentService.complete(paymentNo));
    }

    @PostMapping("/{paymentNo}/fail")
    public ApiResponse<PaymentResponse> fail(
            @PathVariable String paymentNo,
            @Valid @RequestBody FailPaymentRequest request
    ) {
        return ApiResponse.ok(paymentService.fail(paymentNo, request));
    }
}
```

- [ ] **Step 4: Tạo GlobalExceptionHandler**

Tạo `GlobalExceptionHandler.java` với các handler exact:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ");
        return ApiResponse.fail(message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBadRequest(IllegalArgumentException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler({PaymentNotFoundException.class, OrderNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(RuntimeException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(PaymentConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleConflict(PaymentConflictException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(OrderServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiResponse<Void> handleOrderServiceUnavailable(
            OrderServiceUnavailableException exception
    ) {
        return ApiResponse.fail(exception.getMessage());
    }
}
```

- [ ] **Step 5: Chạy controller tests để thấy GREEN**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl payment-service -Dtest=PaymentControllerTest test
```

Expected: payment controller tests pass.

---

### Task 8: Nối Config Server, Gateway và Docker Compose

**Files:**
- Create: `payment-service/src/main/resources/application.yml`
- Create: `environment/config-repo/payment-service-dev.yml`
- Create: `environment/config-repo/payment-service-docker.yml`
- Modify: `environment/config-repo/gateway-dev.yml`
- Modify: `environment/config-repo/gateway-docker.yml`
- Modify: `docker-compose.yml`
- Modify: `.env.example`
- Modify: `.env.dev.example`
- Modify: `.env.prod.example`

**Interfaces:**
- Produces: Config Server label `payment-service-dev` và `payment-service-docker`.
- Produces: Gateway route `/api/payments/** -> lb://PAYMENT-SERVICE`.
- Produces: container `ticket-payment-service`.

- [ ] **Step 1: Tạo application.yml**

```yaml
spring:
  application:
    # Config Server dùng tên này để tìm payment-service-{profile}.yml.
    name: payment-service
  config:
    # Local dùng localhost; Docker ghi đè bằng SPRING_CONFIG_IMPORT.
    import: optional:configserver:http://localhost:8888

server:
  port: 8095

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

- [ ] **Step 2: Tạo config local**

`payment-service-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/payment_service_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

order:
  service:
    base-url: http://localhost:8094
```

- [ ] **Step 3: Tạo config Docker**

`payment-service-docker.yml` giống config local, chỉ đổi:

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/payment_service_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true

eureka:
  client:
    service-url:
      defaultZone: http://discovery:8761/eureka/

order:
  service:
    base-url: http://order-service:8094
```

Giữ nguyên `username`, `password`, `jpa` và `flyway` như file dev.

- [ ] **Step 4: Thêm Gateway route vào cả hai file**

Thêm sau route order-service:

```yaml
- id: payment-service
  uri: lb://PAYMENT-SERVICE
  predicates:
    - Path=/api/payments,/api/payments/**
```

Không route `/internal/orders/**` qua Gateway; API nội bộ chỉ dành cho service-to-service.

- [ ] **Step 5: Thêm PAYMENT_SERVICE_PORT vào env examples**

Thêm:

```dotenv
PAYMENT_SERVICE_PORT=8095
```

- [ ] **Step 6: Thêm service vào docker-compose.yml**

```yaml
payment-service:
  build:
    context: ./payment-service
    dockerfile: Dockerfile
  restart: unless-stopped
  container_name: ticket-payment-service
  profiles: [ "platform" ]
  environment:
    SPRING_PROFILES_ACTIVE: docker
    SPRING_CONFIG_IMPORT: ${CONFIG_IMPORT:-configserver:http://config:8888}
    EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: ${EUREKA_DEFAULT_ZONE:-http://discovery:8761/eureka/}
  ports:
    - "${PAYMENT_SERVICE_PORT:-8095}:8095"
  healthcheck:
    test: [ "CMD-SHELL", "wget -qO- http://localhost:8095/actuator/health || exit 1" ]
    interval: 10s
    timeout: 5s
    retries: 12
  depends_on:
    mysql:
      condition: service_healthy
    discovery:
      condition: service_started
    config:
      condition: service_healthy
    order-service:
      condition: service_healthy
```

Thêm `payment-service: condition: service_healthy` vào `gateway.depends_on`.

- [ ] **Step 7: Verify Config Server output**

Sau khi Config Server chạy:

```powershell
Invoke-RestMethod 'http://localhost:8888/payment-service/dev'
Invoke-RestMethod 'http://localhost:8888/payment-service/docker'
```

Expected: response có datasource, Eureka và `order.service.base-url` đúng profile.

---

### Task 9: Verify full Phase 10 và cập nhật roadmap

**Files:**
- Modify: `docs/ke-hoach-cong-viec.md`

**Interfaces:**
- Consumes: toàn bộ module, config và Docker wiring từ Tasks 1-8.
- Produces: bằng chứng Phase 10 hoàn thành và roadmap đồng bộ.

- [ ] **Step 1: Chạy test hai module thay đổi**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl order-service,payment-service -am clean test
```

Expected: `BUILD SUCCESS`; toàn bộ order-service và payment-service tests pass.

- [ ] **Step 2: Build JAR trước khi Docker build**

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl order-service,payment-service -am clean package -DskipTests
```

Expected:

```text
order-service/target/order-service-1.0-SNAPSHOT.jar
payment-service/target/payment-service-1.0-SNAPSHOT.jar
```

- [ ] **Step 3: Build và chạy Docker stack**

```powershell
docker compose --profile platform up -d --build
docker compose --profile platform ps
```

Expected: `ticket-order-service`, `ticket-payment-service`, `ticket-gateway`, `ticket-config`, `ticket-discovery`, `ticket-mysql` đều `Up`; service có healthcheck phải `healthy`.

- [ ] **Step 4: Kiểm tra Eureka và health**

```powershell
Invoke-RestMethod 'http://localhost:8095/actuator/health'
Invoke-RestMethod 'http://localhost:8080/actuator/health'
```

Expected: cả hai trả `status = UP`; Eureka UI có `PAYMENT-SERVICE`.

- [ ] **Step 5: Chuẩn bị order PENDING_PAYMENT**

Tạo inventory nếu chưa có:

```powershell
$inventoryBody = @{ eventId = 10001; totalQuantity = 10 } | ConvertTo-Json -Compress
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/inventories' -ContentType 'application/json' -Body $inventoryBody
```

Tạo order:

```powershell
$orderBody = @{ userId = 1; eventId = 10001; quantity = 1 } | ConvertTo-Json -Compress
$order = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/orders' -Headers @{ 'Idempotency-Key' = 'phase10-order-complete-001' } -ContentType 'application/json' -Body $orderBody
$order.data
```

Expected: order status `PENDING_PAYMENT` và có `orderNo`.

- [ ] **Step 6: Tạo payment qua Gateway**

```powershell
$paymentBody = @{ orderNo = $order.data.orderNo } | ConvertTo-Json -Compress
$payment = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/payments' -ContentType 'application/json' -Body $paymentBody
$payment.data
```

Expected: payment status `PENDING` và có `paymentNo`.

- [ ] **Step 7: Gọi create lần hai để kiểm tra dùng lại record**

```powershell
$paymentAgain = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/payments' -ContentType 'application/json' -Body $paymentBody
$paymentAgain.data.paymentNo -eq $payment.data.paymentNo
```

Expected: `True`.

- [ ] **Step 8: Complete payment và kiểm tra order**

```powershell
$completed = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/payments/$($payment.data.paymentNo)/complete"
$checkout = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/orders/$($order.data.orderNo)/checkout"
$completed.data.status
$checkout.data.status
```

Expected:

```text
COMPLETED
CONFIRMED
```

- [ ] **Step 9: Gọi complete lần hai để kiểm tra idempotent**

```powershell
$completedAgain = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/payments/$($payment.data.paymentNo)/complete"
$completedAgain.data.status
```

Expected: `COMPLETED`; không phát sinh lỗi.

- [ ] **Step 10: Kiểm tra failure flow bằng order mới**

```powershell
$orderBody2 = @{ userId = 1; eventId = 10001; quantity = 1 } | ConvertTo-Json -Compress
$order2 = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/orders' -Headers @{ 'Idempotency-Key' = 'phase10-order-fail-001' } -ContentType 'application/json' -Body $orderBody2
$paymentBody2 = @{ orderNo = $order2.data.orderNo } | ConvertTo-Json -Compress
$payment2 = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/payments' -ContentType 'application/json' -Body $paymentBody2
$failBody = @{ reason = 'PAYMENT_DECLINED' } | ConvertTo-Json -Compress
$failed = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/payments/$($payment2.data.paymentNo)/fail" -ContentType 'application/json' -Body $failBody
$checkout2 = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/orders/$($order2.data.orderNo)/checkout"
$failed.data.status
$checkout2.data.status
```

Expected:

```text
FAILED
CANCELLED
```

- [ ] **Step 11: Kiểm tra transition ngược trả 409**

```powershell
try {
    Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/payments/$($payment2.data.paymentNo)/complete"
} catch {
    $_.Exception.Response.StatusCode.value__
    $_.ErrorDetails.Message
}
```

Expected: status code `409` và message `Payment FAILED không thể chuyển sang COMPLETED`.

- [ ] **Step 12: Cập nhật roadmap**

Trong `docs/ke-hoach-cong-viec.md`:

```markdown
### Checklist

- [x] Tạo `payment-service`
- [x] migration payments
- [x] API create payment
- [x] API complete/fail payment
- [x] Order internal payment transition API
- [x] Payment gọi Order bằng HTTP đồng bộ
- [x] config/gateway/docker
- [x] test local/docker

### Bằng chứng

- `POST /api/payments` tạo payment `PENDING`.
- Create lặp cùng `orderNo` trả cùng payment.
- Complete payment đổi order sang `CONFIRMED`.
- Fail payment đổi order sang `CANCELLED`.
- Transition ngược trả HTTP `409`.
- `ticket-payment-service` healthy và đăng ký Eureka.

### Trạng thái hiện tại

- `DONE`
```

Đổi API gợi ý từ `{paymentId}` sang `{paymentNo}`.

- [ ] **Step 13: Review diff cuối**

```powershell
git status --short
git diff --check
git diff -- payment-service order-service environment/config-repo docker-compose.yml pom.xml .env.example .env.dev.example .env.prod.example docs/ke-hoach-cong-viec.md docs/superpowers
```

Expected: không có whitespace error; diff chỉ chứa Phase 10 và tài liệu đã duyệt.

---

## Nợ kỹ thuật cố ý chuyển phase sau

- Race khi hai request đồng thời cùng tạo payment cho một order: unique constraint chặn duplicate; mapping đẹp sang `409` chuyển Phase 15.
- DB payment và DB order chưa có distributed transaction: Phase 11 thay bằng Kafka và Outbox.
- Payment thất bại chưa release inventory: Phase 13 xử lý bằng event.
- Order hết hạn chưa tự cancel: Phase 13 thêm scheduler và release inventory.
- HTTP chưa có retry/circuit breaker: đánh giá sau khi event flow hoạt động.
