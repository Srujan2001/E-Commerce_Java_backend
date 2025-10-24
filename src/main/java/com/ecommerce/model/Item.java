package com.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "itemid", columnDefinition = "BINARY(16)")
    private UUID itemId;

    @NotBlank(message = "Item name is required")
    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Item cost is required")
    @Column(name = "item_cost", nullable = false)
    private BigDecimal itemCost;

    @NotNull(message = "Item quantity is required")
    @Column(name = "item_quantity", nullable = false)
    private Integer itemQuantity;

    @NotBlank(message = "Item category is required")
    @Column(name = "item_category", nullable = false)
    private String itemCategory;

    @Column(name = "added_by")
    private String addedBy;

    @Column(name = "imgname")
    private String imgname;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}