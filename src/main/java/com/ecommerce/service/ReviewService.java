package com.ecommerce.service;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.model.Review;
import com.ecommerce.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Transactional
    public ApiResponse addReview(UUID itemId, String reviewText, Integer rating, String userEmail) {
        try {
            Review review = new Review();
            review.setItemId(itemId);
            review.setReviewText(reviewText);
            review.setRating(rating);
            review.setAddedBy(userEmail);

            reviewRepository.save(review);

            return new ApiResponse(true, "Review added successfully", review);
        } catch (Exception e) {
            log.error("Failed to add review", e);
            return new ApiResponse(false, "Failed to add review: " + e.getMessage());
        }
    }

    public List<Review> getReviewsByItemId(UUID itemId) {
        return reviewRepository.findByItemId(itemId);
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    @Transactional
    public ApiResponse deleteReview(Long reviewId) {
        try {
            reviewRepository.deleteById(reviewId);
            return new ApiResponse(true, "Review deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete review", e);
            return new ApiResponse(false, "Failed to delete review: " + e.getMessage());
        }
    }
}