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

    public Long getId() {
        return id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void reserve(Integer quantity) {
        validateQuantity(quantity);

        if (availableQuantity < quantity) {
            throw new IllegalArgumentException("Không đủ vé để giữ chỗ");
        }

        availableQuantity -= quantity;
        updatedAt = LocalDateTime.now();
    }

    public void release(Integer quantity) {
        validateQuantity(quantity);

        if (availableQuantity + quantity > totalQuantity) {
            throw new IllegalArgumentException(
                    "Số vé trả lại vượt quá tổng số vé"
            );
        }

        availableQuantity += quantity;
        updatedAt = LocalDateTime.now();
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Số lượng phải lớn hơn 0"
            );
        }
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