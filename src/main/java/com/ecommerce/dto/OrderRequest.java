package com.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderRequest {
    private UUID itemId;
    private String itemName;
    private BigDecimal total;
    private Integer quantity;
    private String paymentId;
    private String orderId;
    private String signature;
}