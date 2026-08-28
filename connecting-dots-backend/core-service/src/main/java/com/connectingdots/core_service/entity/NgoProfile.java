package com.connectingdots.core_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ngo_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NgoProfile extends BaseEntity {

    // The foreign key linking back to the users table
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(nullable = false)
    private String domain; 

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "preferred_language")
    @Builder.Default
    private String preferredLanguage = "en";

    @Column(name = "is_verified", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isVerified = false;
}