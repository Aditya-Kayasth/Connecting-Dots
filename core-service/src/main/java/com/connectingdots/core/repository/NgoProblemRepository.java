package com.connectingdots.core.repository;

import com.connectingdots.core.domain.NgoProblemStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NgoProblemRepository extends JpaRepository<NgoProblemStatement, UUID> {
    List<NgoProblemStatement> findByStatus(String status);
}
