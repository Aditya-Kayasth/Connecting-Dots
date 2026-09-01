package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.ReviewRequest;
import com.connectingdots.core_service.entity.Application;
import com.connectingdots.core_service.entity.ContributorProfile;
import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.entity.Review;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.ApplicationRepository;
import com.connectingdots.core_service.repository.ContributorProfileRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.ReviewRepository;
import com.connectingdots.core_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProblemStatementRepository problemStatementRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final ContributorProfileRepository contributorProfileRepository;

    @Transactional
    public Review createReview(ReviewRequest request, UUID explicitRaterId) {
        UUID raterId = explicitRaterId;
        if (raterId == null) {
            raterId = getAuthenticatedUserId();
        }

        UUID problemId = request.problemId();
        UUID rateeId = request.rateeId();

        if (request.applicationId() != null) {
            // Retrieve from application
            Application application = applicationRepository.findById(request.applicationId())
                    .orElseThrow(() -> new IllegalArgumentException("Application not found"));

            // Check if application is completed
            if (!"COMPLETED".equalsIgnoreCase(application.getStatus())) {
                throw new IllegalStateException("You can only review completed collaborations.");
            }

            problemId = application.getProblemId();

            // Find rater role to determine ratee
            User raterUser = userRepository.findById(raterId)
                    .orElseThrow(() -> new RuntimeException("Rater not found"));

            if (raterUser.getRole() == User.Role.NGO) {
                // Rater is NGO, so ratee must be the Contributor
                ContributorProfile contributor = contributorProfileRepository.findById(application.getContributorProfileId())
                        .orElseThrow(() -> new RuntimeException("Contributor profile not found"));
                rateeId = contributor.getUser().getId();
            } else if (raterUser.getRole() == User.Role.CONTRIBUTOR) {
                // Rater is Contributor, so ratee must be the NGO
                ProblemStatement problem = problemStatementRepository.findById(application.getProblemId())
                        .orElseThrow(() -> new RuntimeException("Problem statement not found"));
                rateeId = problem.getNgoProfile().getUser().getId();
            } else {
                throw new IllegalStateException("Admins cannot submit collaboration reviews.");
            }
        }

        // Prevent self-reviews
        if (raterId.equals(rateeId)) {
            throw new IllegalArgumentException("You cannot review yourself.");
        }

        // Validate problem statement exists
        if (problemId == null || !problemStatementRepository.existsById(problemId)) {
            throw new IllegalArgumentException("Problem statement not found");
        }

        // Validate ratee exists
        if (rateeId == null || !userRepository.existsById(rateeId)) {
            throw new IllegalArgumentException("Ratee user not found");
        }

        // Check if review already exists to prevent duplicate review spamming
        if (reviewRepository.existsByProblemIdAndRaterIdAndRateeId(problemId, raterId, rateeId)) {
            throw new IllegalStateException("You have already reviewed this collaboration.");
        }

        // Create review
        Review review = Review.builder()
                .problemId(problemId)
                .raterId(raterId)
                .rateeId(rateeId)
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
