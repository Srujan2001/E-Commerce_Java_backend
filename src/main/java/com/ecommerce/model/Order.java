package com.ecommerce.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "item_id", columnDefinition = "BINARY(16)")
    private UUID itemId;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "total")
    private BigDecimal total;

    @Column(name = "payment_by")
    private String paymentBy;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "order_status")
    private String orderStatus = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}