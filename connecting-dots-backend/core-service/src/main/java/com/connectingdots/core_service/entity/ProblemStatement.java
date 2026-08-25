package com.connectingdots.core_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "problem_statements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemStatement extends BaseEntity {

    // The foreign key linking back to the specific NGO that posted this
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ngo_profile_id", referencedColumnName = "id", nullable = false)
    private NgoProfile ngoProfile;

    @Column(nullable = false)
    private String title;

    // We use TEXT instead of VARCHAR so the NGO can write a highly detailed description
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    // Categories like "Data Science", "Machine Learning", or "Web Development"
    @Column(nullable = false)
    private String domain; 

    @Column(name = "source_file_url")
    private String sourceFileUrl;

    @Column(name = "source_type")
    private String sourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.OPEN;

    public enum Status {
        UPLOADED, PROCESSING, DRAFT, OPEN, IN_PROGRESS, CLOSED, RESOLVED, PROCESSED
    }
}