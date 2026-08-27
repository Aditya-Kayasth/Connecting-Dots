package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.ReviewRequest;
import com.connectingdots.core_service.entity.Review;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.ReviewRepository;
import com.connectingdots.core_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProblemStatementRepository problemStatementRepository;
    private final UserRepository userRepository;

    public Review createReview(ReviewRequest request, UUID explicitRaterId) {
        UUID raterId = explicitRaterId;
        if (raterId == null) {
            raterId = getAuthenticatedUserId();
        }

        // Validate problem statement exists
        if (!problemStatementRepository.existsById(request.problemId())) {
            throw new RuntimeException("Problem statement not found");
        }

        // Validate ratee exists
        if (!userRepository.existsById(request.rateeId())) {
            throw new RuntimeException("Ratee user not found");
        }

        // Create review
        Review review = Review.builder()
                .problemId(request.problemId())
                .raterId(raterId)
                .rateeId(request.rateeId())
                .rating(request.rating())
                .comment(request.comment())
                .build();

        return reviewRepository.save(review);
    }

    public List<Review> getReviewsForUser(UUID userId) {
        return reviewRepository.findByRateeId(userId);
    }

    private UUID getAuthenticatedUserId() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new SecurityException("User is not authenticated");
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
