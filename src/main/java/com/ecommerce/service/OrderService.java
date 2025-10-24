package com.ecommerce.service;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.model.Order;
import com.ecommerce.repository.OrderRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public JSONObject createRazorpayOrder(OrderRequest orderRequest) throws RazorpayException {
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        JSONObject orderRequestJson = new JSONObject();
        orderRequestJson.put("amount", orderRequest.getTotal().multiply(java.math.BigDecimal.valueOf(100)).intValue());
        orderRequestJson.put("currency", "INR");
        orderRequestJson.put("receipt", "order_" + System.currentTimeMillis());
        orderRequestJson.put("payment_capture", 1);

        com.razorpay.Order order = client.orders.create(orderRequestJson);

        log.info("Razorpay order created: {}", order.toString());

        return new JSONObject(order.toString());
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            return Utils.verifyPaymentSignature(options, razorpayKeySecret);
        } catch (RazorpayException e) {
            log.error("Payment signature verification failed", e);
            return false;
        }
    }

    @Transactional
    public ApiResponse createOrder(OrderRequest orderRequest, String userEmail) {
        try {
            Order order = new Order();
            order.setItemId(orderRequest.getItemId());
            order.setItemName(orderRequest.getItemName());
            order.setTotal(orderRequest.getTotal());
            order.setPaymentBy(userEmail);
            order.setPaymentId(orderRequest.getPaymentId());
            order.setOrderStatus("COMPLETED");

            orderRepository.save(order);

            return new ApiResponse(true, "Order placed successfully", order);
        } catch (Exception e) {
            log.error("Failed to create order", e);
            return new ApiResponse(false, "Failed to create order: " + e.getMessage());
        }
    }

    public List<Order> getOrdersByUser(String userEmail) {
        return orderRepository.findByPaymentBy(userEmail);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}