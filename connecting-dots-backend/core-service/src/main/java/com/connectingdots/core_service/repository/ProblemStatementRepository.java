package com.connectingdots.core_service.repository;

import com.connectingdots.core_service.entity.ProblemStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface ProblemStatementRepository extends JpaRepository<ProblemStatement, UUID>, JpaSpecificationExecutor<ProblemStatement> {
}