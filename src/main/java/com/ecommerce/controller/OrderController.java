package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.model.Order;
import com.ecommerce.service.OrderService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest orderRequest) {
        try {
            JSONObject razorpayOrder = orderService.createRazorpayOrder(orderRequest);
            return ResponseEntity.ok(new ApiResponse(true, "Order created", razorpayOrder.toMap()));
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to create order"));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyPayment(@RequestBody OrderRequest orderRequest,
                                                     Authentication authentication) {
        try {
            String userEmail = authentication.getName();

            // Verify payment signature
            boolean isValid = orderService.verifyPaymentSignature(
                    orderRequest.getOrderId(),
                    orderRequest.getPaymentId(),
                    orderRequest.getSignature()
            );

            if (!isValid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "Payment verification failed"));
            }

            // Create order in database
            ApiResponse response = orderService.createOrder(orderRequest, userEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to verify payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to verify payment"));
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            List<Order> orders = orderService.getOrdersByUser(userEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Orders fetched successfully", orders));
        } catch (Exception e) {
            log.error("Failed to fetch orders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to fetch orders"));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId,
                                          Authentication authentication) {
        try {
            Order order = orderService.getOrderById(orderId);
            String userEmail = authentication.getName();

            // Check if user owns this order
            if (!order.getPaymentBy().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Unauthorized access"));
            }

            return ResponseEntity.ok(new ApiResponse(true, "Order fetched successfully", order));
        } catch (Exception e) {
            log.error("Failed to fetch order", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Order not found"));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            return ResponseEntity.ok(new ApiResponse(true, "Orders fetched successfully", orders));
        } catch (Exception e) {
            log.error("Failed to fetch orders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to fetch orders"));
        }
    }
}