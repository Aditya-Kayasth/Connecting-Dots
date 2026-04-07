package com.connectingdots.domain;

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

    private String status = "OPEN";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimed_by_id")
    private User claimedBy;

    public NgoProblemStatement() {}

    public NgoProblemStatement(UUID id, String ngoName, String rawDescription,
                                String structuredProblem, TechCategory techCategory, String status) {
        this.id = id;
        this.ngoName = ngoName;
        this.rawDescription = rawDescription;
        this.structuredProblem = structuredProblem;
        this.techCategory = techCategory;
        this.status = status;
    }

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

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public User getClaimedBy() { return claimedBy; }
    public void setClaimedBy(User claimedBy) { this.claimedBy = claimedBy; }
}
