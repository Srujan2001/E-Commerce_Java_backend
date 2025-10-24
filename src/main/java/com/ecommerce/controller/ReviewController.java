package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.model.Review;
import com.ecommerce.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addReview(@RequestBody Map<String, Object> request,
                                                 Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            UUID itemId = UUID.fromString((String) request.get("itemId"));
            String reviewText = (String) request.get("reviewText");
            Integer rating = (Integer) request.get("rating");

            ApiResponse response = reviewService.addReview(itemId, reviewText, rating, userEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to add review", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to add review: " + e.getMessage()));
        }
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<?> getReviewsByItemId(@PathVariable UUID itemId) {
        try {
            List<Review> reviews = reviewService.getReviewsByItemId(itemId);
            return ResponseEntity.ok(new ApiResponse(true, "Reviews fetched successfully", reviews));
        } catch (Exception e) {
            log.error("Failed to fetch reviews", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to fetch reviews"));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllReviews() {
        try {
            List<Review> reviews = reviewService.getAllReviews();
            return ResponseEntity.ok(new ApiResponse(true, "Reviews fetched successfully", reviews));
        } catch (Exception e) {
            log.error("Failed to fetch reviews", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to fetch reviews"));
        }
    }

    @DeleteMapping("/delete/{reviewId}")
    public ResponseEntity<ApiResponse> deleteReview(@PathVariable Long reviewId) {
        try {
            ApiResponse response = reviewService.deleteReview(reviewId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete review", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to delete review"));
        }
    }
}