package com.connectingdots.core_service.repository;

import com.connectingdots.core_service.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByRateeId(UUID rateeId);
    boolean existsByProblemIdAndRaterIdAndRateeId(UUID problemId, UUID raterId, UUID rateeId);
}
