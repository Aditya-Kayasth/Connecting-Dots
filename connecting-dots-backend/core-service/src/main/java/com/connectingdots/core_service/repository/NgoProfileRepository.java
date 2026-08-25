package com.connectingdots.core_service.repository;

import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NgoProfileRepository extends JpaRepository<NgoProfile, UUID> {
    
    // Spring Data JPA automatically writes the SQL query for this behind the scenes!
    Optional<NgoProfile> findByUser(User user);
}