package com.connectingdots.core_service.repository;

import com.connectingdots.core_service.entity.ContributorProfile;
import com.connectingdots.core_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContributorProfileRepository extends JpaRepository<ContributorProfile, UUID> {
    Optional<ContributorProfile> findByUser(User user);
    Optional<ContributorProfile> findByUserId(UUID userId);
}