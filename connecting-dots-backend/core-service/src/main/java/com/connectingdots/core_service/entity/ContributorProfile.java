package com.connectingdots.core_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contributor_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributorProfile extends BaseEntity {

    // The foreign key linking back to the users table
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "skills_summary")
    private String skillsSummary; 

    @Column(name = "portfolio_url")
    private String portfolioUrl;

    @Column(name = "preferred_language")
    @Builder.Default
    private String preferredLanguage = "en";

    @Column(name = "completed_projects")
    @Builder.Default
    private Integer completedProjects = 0;
}