# Inventory v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reserve/release inventory operations that prevent oversell in one database.

**Architecture:** `inventory-service` remains a Spring Boot service with controller/service/repository/entity layers. Reserve and release run inside transactions and load inventory rows with `PESSIMISTIC_WRITE` so concurrent requests serialize per `eventId`.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring Data JPA, MySQL 8.4, Flyway, Maven, Docker Compose.

## Global Constraints

- Keep scope to Inventory v1 only: no idempotency key, no reservation record, no Kafka/outbox, no timeout release.
- Use existing `ApiResponse.ok(...)` and `ApiResponse.fail(...)` envelope.
- No new dependencies.
- Use `PESSIMISTIC_WRITE` for reserve/release row locking.
- API paths are `POST /api/inventories/{eventId}/reserve` and `POST /api/inventories/{eventId}/release`.
- Shared request body is `{ "quantity": 2 }`.

---

## File Structure

- `inventory-service/src/main/java/com/ticketsale/inventory/controller/dto/request/ChangeInventoryRequest.java`: request DTO for reserve/release quantity.
- `inventory-service/src/main/java/com/ticketsale/inventory/repository/entity/InventoryEntity.java`: owns inventory state mutation and boundary checks.
- `inventory-service/src/main/java/com/ticketsale/inventory/repository/InventoryRepository.java`: adds row-locking finder for write operations.
- `inventory-service/src/main/java/com/ticketsale/inventory/service/InventoryService.java`: exposes reserve/release service methods.
- `inventory-service/src/main/java/com/ticketsale/inventory/service/impl/InventoryServiceImpl.java`: implements transactional reserve/release.
- `inventory-service/src/main/java/com/ticketsale/inventory/controller/InventoryController.java`: exposes HTTP endpoints.
- `inventory-service/src/test/java/com/ticketsale/inventory/controller/InventoryControllerTest.java`: controller tests for endpoints.
- `inventory-service/src/test/java/com/ticketsale/inventory/service/InventoryServiceImplTest.java`: service tests for business rules.
- `docs/ke-hoach-cong-viec.md`: update Phase 7 checklist after verification.

---

### Task 1: Add Reserve/Release Request DTO And Controller Endpoints

**Files:**
- Create: `inventory-service/src/main/java/com/ticketsale/inventory/controller/dto/request/ChangeInventoryRequest.java`
- Modify: `inventory-service/src/main/java/com/ticketsale/inventory/controller/InventoryController.java`
- Modify: `inventory-service/src/main/java/com/ticketsale/inventory/service/InventoryService.java`
- Test: `inventory-service/src/test/java/com/ticketsale/inventory/controller/InventoryControllerTest.java`

**Interfaces:**
- Consumes: existing `InventoryResponse` record.
- Produces: `ChangeInventoryRequest(Integer quantity)`, `InventoryService.reserve(Long eventId, Integer quantity)`, `InventoryService.release(Long eventId, Integer quantity)`.

- [ ] **Step 1: Add controller tests for reserve and release endpoints**

Replace `inventory-service/src/test/java/com/ticketsale/inventory/controller/InventoryControllerTest.java` with:

```java
package com.ticketsale.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketsale.inventory.controller.dto.response.InventoryResponse;
import com.ticketsale.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    @Test
    void createShouldReturnInventory() throws Exception {
        Mockito.when(inventoryService.create(any()))
                .thenReturn(new InventoryResponse(
                        1L,
                        1L,
                        100,
                        100,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/api/inventories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateTestRequest(1L, 100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value(1))
                .andExpect(jsonPath("$.data.totalQuantity").value(100));
    }

    @Test
    void reserveShouldReturnUpdatedInventory() throws Exception {
        Mockito.when(inventoryService.reserve(eq(1L), eq(2)))
                .thenReturn(new InventoryResponse(
                        1L,
                        1L,
                        100,
                        98,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/api/inventories/1/reserve")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ChangeTestRequest(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableQuantity").value(98));
    }

    @Test
    void releaseShouldReturnUpdatedInventory() throws Exception {
        Mockito.when(inventoryService.release(eq(1L), eq(2)))
                .thenReturn(new InventoryResponse(
                        1L,
                        1L,
                        100,
                        100,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/api/inventories/1/release")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ChangeTestRequest(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableQuantity").value(100));
    }

    private record CreateTestRequest(Long eventId, Integer totalQuantity) {
    }

    private record ChangeTestRequest(Integer quantity) {
    }
}
```

- [ ] **Step 2: Run controller test and verify compile fails**

Run:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl inventory-service -Dtest=InventoryControllerTest test
```

Expected: FAIL because `InventoryService.reserve(...)` and `InventoryService.release(...)` do not exist.

- [ ] **Step 3: Add request DTO**

Create `inventory-service/src/main/java/com/ticketsale/inventory/controller/dto/request/ChangeInventoryRequest.java`:

```java
package com.ticketsale.inventory.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChangeInventoryRequest(
        @NotNull(message = "Sá»‘ lÆ°á»£ng khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng")
        @Min(value = 1, message = "Sá»‘ lÆ°á»£ng pháº£i lá»›n hÆ¡n 0")
        Integer quantity
) {
}
```

- [ ] **Step 4: Extend service interface**

Modify `inventory-service/src/main/java/com/ticketsale/inventory/service/InventoryService.java`:

```java
package com.ticketsale.inventory.service;

import com.ticketsale.inventory.controller.dto.request.CreateInventoryRequest;
import com.ticketsale.inventory.controller.dto.response.InventoryResponse;

public interface InventoryService {

    InventoryResponse create(CreateInventoryRequest request);

    InventoryResponse getByEventId(Long eventId);

    InventoryResponse reserve(Long eventId, Integer quantity);

    InventoryResponse release(Long eventId, Integer quantity);
}
```

- [ ] **Step 5: Add controller endpoints**

Modify `inventory-service/src/main/java/com/ticketsale/inventory/controller/InventoryController.java`:

```java
package com.ticketsale.inventory.controller;

import com.ticketsale.common.response.ApiResponse;
import com.ticketsale.inventory.controller.dto.request.ChangeInventoryRequest;
import com.ticketsale.inventory.controller.dto.request.CreateInventoryRequest;
import com.ticketsale.inventory.controller.dto.response.InventoryResponse;
import com.ticketsale.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventories")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ApiResponse<InventoryResponse> create(@Valid @RequestBody CreateInventoryRequest request) {
        return ApiResponse.ok(inventoryService.create(request));
    }

    @GetMapping("/{eventId}")
    public ApiResponse<InventoryResponse> getByEventId(@PathVariable Long eventId) {
        return ApiResponse.ok(inventoryService.getByEventId(eventId));
    }

    @PostMapping("/{eventId}/reserve")
    public ApiResponse<InventoryResponse> reserve(
            @PathVariable Long eventId,
            @Valid @RequestBody ChangeInventoryRequest request
    ) {
        return ApiResponse.ok(inventoryService.reserve(eventId, request.quantity()));
    }

    @PostMapping("/{eventId}/release")
    public ApiResponse<InventoryResponse> release(
            @PathVariable Long eventId,
            @Valid @RequestBody ChangeInventoryRequest request
    ) {
        return ApiResponse.ok(inventoryService.release(eventId, request.quantity()));
    }
}
```

- [ ] **Step 6: Run controller test and verify pass**

Run:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl inventory-service -Dtest=InventoryControllerTest test
```

Expected: PASS, because service is mocked.

---

### Task 2: Add Inventory Mutation Logic And Pessimistic Lock

**Files:**
- Modify: `inventory-service/src/main/java/com/ticketsale/inventory/repository/entity/InventoryEntity.java`
- Modify: `inventory-service/src/main/java/com/ticketsale/inventory/repository/InventoryRepository.java`
- Modify: `inventory-service/src/main/java/com/ticketsale/inventory/service/impl/InventoryServiceImpl.java`
- Test: `inventory-service/src/test/java/com/ticketsale/inventory/service/InventoryServiceImplTest.java`

**Interfaces:**
- Consumes: `InventoryService.reserve(Long eventId, Integer quantity)`, `InventoryService.release(Long eventId, Integer quantity)` from Task 1.
- Produces: working transactional reserve/release using `InventoryRepository.findByEventIdForUpdate(Long eventId)`.

- [ ] **Step 1: Add service tests for reserve/release rules**

Create `inventory-service/src/test/java/com/ticketsale/inventory/service/InventoryServiceImplTest.java`:

```java
package com.ticketsale.inventory.service;

import com.ticketsale.inventory.controller.dto.request.CreateInventoryRequest;
import com.ticketsale.inventory.controller.dto.response.InventoryResponse;
import com.ticketsale.inventory.repository.InventoryRepository;
import com.ticketsale.inventory.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    void reserveShouldDecreaseAvailableQuantity() {
        when(inventoryRepository.findByEventIdForUpdate(1L))
                .thenReturn(Optional.of(newInventory(1L, 100)));

        InventoryResponse response = inventoryService.reserve(1L, 2);

        assertThat(response.availableQuantity()).isEqualTo(98);
    }

    @Test
    void reserveShouldFailWhenAvailableQuantityIsNotEnough() {
        when(inventoryRepository.findByEventIdForUpdate(1L))
                .thenReturn(Optional.of(newInventory(1L, 1)));

        assertThatThrownBy(() -> inventoryService.reserve(1L, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("KhÃ´ng Ä‘á»§ vÃ© Ä‘á»ƒ giá»¯ chá»—");
    }

    @Test
    void reserveShouldFailWhenQuantityIsZero() {
        when(inventoryRepository.findByEventIdForUpdate(1L))
                .thenReturn(Optional.of(newInventory(1L, 100)));

        assertThatThrownBy(() -> inventoryService.reserve(1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Số lượng phải lớn hơn 0");
    }

    @Test
    void releaseShouldIncreaseAvailableQuantity() {
        when(inventoryRepository.findByEventIdForUpdate(1L))
                .thenReturn(Optional.of(newInventoryAfterReserve(1L, 100, 98)));

        InventoryResponse response = inventoryService.release(1L, 2);

        assertThat(response.availableQuantity()).isEqualTo(100);
    }

    @Test
    void releaseShouldFailWhenQuantityExceedsTotalQuantity() {
        when(inventoryRepository.findByEventIdForUpdate(1L))
                .thenReturn(Optional.of(newInventory(1L, 100)));

        assertThatThrownBy(() -> inventoryService.release(1L, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sá»‘ vÃ© tráº£ láº¡i vÆ°á»£t quÃ¡ tá»•ng sá»‘ vÃ©");
    }

    private static com.ticketsale.inventory.repository.entity.InventoryEntity newInventory(Long eventId, Integer totalQuantity) {
        return new com.ticketsale.inventory.repository.entity.InventoryEntity(eventId, totalQuantity);
    }

    private static com.ticketsale.inventory.repository.entity.InventoryEntity newInventoryAfterReserve(
            Long eventId,
            Integer totalQuantity,
            Integer availableQuantity
    ) {
        com.ticketsale.inventory.repository.entity.InventoryEntity inventory = new com.ticketsale.inventory.repository.entity.InventoryEntity(eventId, totalQuantity);
        inventory.reserve(totalQuantity - availableQuantity);
        return inventory;
    }
}
```

- [ ] **Step 2: Run service test and verify compile fails**

Run:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl inventory-service -Dtest=InventoryServiceImplTest test
```

Expected: FAIL because `reserve(...)`, `release(...)`, and `findByEventIdForUpdate(...)` are missing.

- [ ] **Step 3: Add entity mutation methods**

Modify `inventory-service/src/main/java/com/ticketsale/inventory/repository/entity/InventoryEntity.java`:

```java
package com.ticketsale.inventory.repository.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventories")
public class InventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private Long eventId;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected InventoryEntity() {
    }

    public InventoryEntity(Long eventId, Integer totalQuantity) {
        this.eventId = eventId;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = totalQuantity;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void reserve(Integer quantity) {
        validateQuantity(quantity);
        if (availableQuantity < quantity) {
            throw new IllegalArgumentException("KhÃ´ng Ä‘á»§ vÃ© Ä‘á»ƒ giá»¯ chá»—");
        }

        availableQuantity -= quantity;
        updatedAt = LocalDateTime.now();
    }

    public void release(Integer quantity) {
        validateQuantity(quantity);
        if (availableQuantity + quantity > totalQuantity) {
            throw new IllegalArgumentException("Sá»‘ vÃ© tráº£ láº¡i vÆ°á»£t quÃ¡ tá»•ng sá»‘ vÃ©");
        }

        availableQuantity += quantity;
        updatedAt = LocalDateTime.now();
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getEventId() {
        return eventId;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
```

- [ ] **Step 4: Add pessimistic lock repository method**

Modify `inventory-service/src/main/java/com/ticketsale/inventory/repository/InventoryRepository.java`:

```java
package com.ticketsale.inventory.repository;

import com.ticketsale.inventory.repository.entity.InventoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findByEventId(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryEntity i where i.eventId = :eventId")
    Optional<InventoryEntity> findByEventIdForUpdate(@Param("eventId") Long eventId);
}
```

- [ ] **Step 5: Implement service reserve/release**

Modify `inventory-service/src/main/java/com/ticketsale/inventory/service/impl/InventoryServiceImpl.java`:

```java
package com.ticketsale.inventory.service.impl;

import com.ticketsale.inventory.controller.dto.request.CreateInventoryRequest;
import com.ticketsale.inventory.controller.dto.response.InventoryResponse;
import com.ticketsale.inventory.repository.InventoryRepository;
import com.ticketsale.inventory.repository.entity.InventoryEntity;
import com.ticketsale.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional
    public InventoryResponse create(CreateInventoryRequest request) {
        inventoryRepository.findByEventId(request.eventId()).ifPresent(existing -> {
            throw new IllegalArgumentException("Inventory cá»§a event Ä‘Ã£ tá»“n táº¡i");
        });

        InventoryEntity saved = inventoryRepository.save(new InventoryEntity(
                request.eventId(),
                request.totalQuantity()
        ));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getByEventId(Long eventId) {
        InventoryEntity entity = inventoryRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y inventory cá»§a event"));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public InventoryResponse reserve(Long eventId, Integer quantity) {
        InventoryEntity entity = inventoryRepository.findByEventIdForUpdate(eventId)
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y inventory cá»§a event"));
        entity.reserve(quantity);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public InventoryResponse release(Long eventId, Integer quantity) {
        InventoryEntity entity = inventoryRepository.findByEventIdForUpdate(eventId)
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y inventory cá»§a event"));
        entity.release(quantity);
        return toResponse(entity);
    }

    private InventoryResponse toResponse(InventoryEntity entity) {
        return new InventoryResponse(
                entity.getId(),
                entity.getEventId(),
                entity.getTotalQuantity(),
                entity.getAvailableQuantity(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 6: Run service test and verify pass**

Run:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl inventory-service -Dtest=InventoryServiceImplTest test
```

Expected: PASS.

---

### Task 3: Verify Full Inventory v1 And Update Roadmap

**Files:**
- Modify: `docs/ke-hoach-cong-viec.md`

**Interfaces:**
- Consumes: working API from Tasks 1-2.
- Produces: verified Inventory v1 and updated roadmap.

- [ ] **Step 1: Run full inventory-service tests**

Run:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl inventory-service clean test
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: Build inventory jar**

Run:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -pl inventory-service clean package -DskipTests
```

Expected: `inventory-service/target/inventory-service-1.0-SNAPSHOT.jar` exists.

- [ ] **Step 3: Rebuild and start inventory-service and gateway**

Run:

```powershell
docker compose --profile platform build --no-cache inventory-service
docker compose --profile platform up -d inventory-service gateway
docker compose --profile platform ps
```

Expected: `ticket-inventory-service` and `ticket-gateway` are healthy.

- [ ] **Step 4: Create test inventory via gateway**

Run:

```powershell
$body = @{ eventId = 701; totalQuantity = 10 } | ConvertTo-Json -Compress
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/inventories' -ContentType 'application/json' -Body $body
```

Expected: response `data.availableQuantity = 10`.

- [ ] **Step 5: Reserve two tickets**

Run:

```powershell
$body = @{ quantity = 2 } | ConvertTo-Json -Compress
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/inventories/701/reserve' -ContentType 'application/json' -Body $body
```

Expected: response `data.availableQuantity = 8`.

- [ ] **Step 6: Release one ticket**

Run:

```powershell
$body = @{ quantity = 1 } | ConvertTo-Json -Compress
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/inventories/701/release' -ContentType 'application/json' -Body $body
```

Expected: response `data.availableQuantity = 9`.

- [ ] **Step 7: Verify reserve cannot oversell**

Run:

```powershell
$body = @{ quantity = 99 } | ConvertTo-Json -Compress
try {
  Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/inventories/701/reserve' -ContentType 'application/json' -Body $body
} catch {
  $_.ErrorDetails.Message
}
```

Expected: HTTP 400 with message `KhÃ´ng Ä‘á»§ vÃ© Ä‘á»ƒ giá»¯ chá»—`.

- [ ] **Step 8: Update roadmap**

In `docs/ke-hoach-cong-viec.md`, tick:

```markdown
- [x] ThÃªm reserve/release cÆ¡ báº£n
```

Append evidence under Phase 7:

```markdown
- `POST /api/inventories/{eventId}/reserve` OK
- `POST /api/inventories/{eventId}/release` OK
- reserve quÃ¡ sá»‘ vÃ© tráº£ 400 OK
```

- [ ] **Step 9: Run git diff review**

Run:

```powershell
git diff -- inventory-service docs/ke-hoach-cong-viec.md docs/superpowers
```

Expected: only Inventory v1 and docs changes.
