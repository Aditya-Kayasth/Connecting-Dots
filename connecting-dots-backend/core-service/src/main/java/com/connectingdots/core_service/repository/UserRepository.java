package com.connectingdots.core_service.repository;

import com.connectingdots.core_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Spring magically translates this method name into a SQL query!
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}