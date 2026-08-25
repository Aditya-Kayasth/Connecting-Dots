package com.connectingdots.core_service.repository;

import com.connectingdots.core_service.entity.ContributorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContributorProfileRepository extends JpaRepository<ContributorProfile, UUID> {
    
}