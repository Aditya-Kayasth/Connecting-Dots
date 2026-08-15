package com.connectingdots.core.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class NgoProblemStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String ngoName;

    @Column(columnDefinition = "TEXT")
    private String rawDescription;

    @Column(columnDefinition = "TEXT")
    private String structuredProblem;

    @Enumerated(EnumType.STRING)
    private TechCategory techCategory;

    // Status: OPEN, INTERESTED, WORK_IN_PROGRESS, COMPLETED
    private String status = "OPEN";

    // Geographic fields
    private Double latitude;
    private Double longitude;
    private String proximityZone;

    private UUID authorId;
    private UUID claimedById;

    public NgoProblemStatement() {}

    // Manual Getters and Setters to avoid Lombok JDK25 compiler issue
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNgoName() { return ngoName; }
    public void setNgoName(String ngoName) { this.ngoName = ngoName; }

    public String getRawDescription() { return rawDescription; }
    public void setRawDescription(String rawDescription) { this.rawDescription = rawDescription; }

    public String getStructuredProblem() { return structuredProblem; }
    public void setStructuredProblem(String structuredProblem) { this.structuredProblem = structuredProblem; }

    public TechCategory getTechCategory() { return techCategory; }
    public void setTechCategory(TechCategory techCategory) { this.techCategory = techCategory; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getProximityZone() { return proximityZone; }
    public void setProximityZone(String proximityZone) { this.proximityZone = proximityZone; }

    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }

    public UUID getClaimedById() { return claimedById; }
    public void setClaimedById(UUID claimedById) { this.claimedById = claimedById; }

    // Builder pattern equivalent
    public static NgoProblemStatementBuilder builder() {
        return new NgoProblemStatementBuilder();
    }

    public static class NgoProblemStatementBuilder {
        private String ngoName;
        private String rawDescription;
        private String structuredProblem;
        private TechCategory techCategory;
        private String status = "OPEN";
        private Double latitude;
        private Double longitude;
        private String proximityZone;

        public NgoProblemStatementBuilder ngoName(String ngoName) { this.ngoName = ngoName; return this; }
        public NgoProblemStatementBuilder rawDescription(String rawDescription) { this.rawDescription = rawDescription; return this; }
        public NgoProblemStatementBuilder structuredProblem(String structuredProblem) { this.structuredProblem = structuredProblem; return this; }
        public NgoProblemStatementBuilder techCategory(TechCategory techCategory) { this.techCategory = techCategory; return this; }
        public NgoProblemStatementBuilder status(String status) { this.status = status; return this; }
        public NgoProblemStatementBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public NgoProblemStatementBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public NgoProblemStatementBuilder proximityZone(String proximityZone) { this.proximityZone = proximityZone; return this; }

        public NgoProblemStatement build() {
            NgoProblemStatement problem = new NgoProblemStatement();
            problem.setNgoName(this.ngoName);
            problem.setRawDescription(this.rawDescription);
            problem.setStructuredProblem(this.structuredProblem);
            problem.setTechCategory(this.techCategory);
            problem.setStatus(this.status);
            problem.setLatitude(this.latitude);
            problem.setLongitude(this.longitude);
            problem.setProximityZone(this.proximityZone);
            return problem;
        }
    }
}
