package com.connectingdots.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NgoProblemRepository extends JpaRepository<NgoProblemStatement, UUID> {
    List<NgoProblemStatement> findByStatus(String status);
}
