package com.connectingdots.core_service.repository;

import com.connectingdots.core_service.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    List<Application> findByContributorProfileId(UUID contributorProfileId);
    List<Application> findByProblemId(UUID problemId);
    boolean existsByProblemIdAndContributorProfileId(UUID problemId, UUID contributorProfileId);
}