package com.connectingdots.core_service.controller;

import com.connectingdots.core_service.dto.ReviewRequest;
import com.connectingdots.core_service.entity.Review;
import com.connectingdots.core_service.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/core")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reviews")
    public ResponseEntity<Review> submitReview(@Valid @RequestBody ReviewRequest request) {
        Review review = reviewService.createReview(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @GetMapping("/users/{userId}/reviews")
    public ResponseEntity<List<Review>> getReviewsForUser(@PathVariable UUID userId) {
        List<Review> reviews = reviewService.getReviewsForUser(userId);
        return ResponseEntity.ok(reviews);
    }
}
