package com.connectingdots.core_service.repository;

import com.connectingdots.core_service.entity.ProblemStatement;
import org.springframework.data.jpa.domain.Specification;

public class ProblemStatementSpecs {

    public static Specification<ProblemStatement> hasDomain(String domain) {
        return (root, query, criteriaBuilder) -> {
            if (domain == null || domain.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("domain"), domain);
        };
    }

    public static Specification<ProblemStatement> hasStatus(String status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null || status.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            try {
                ProblemStatement.Status enumStatus = ProblemStatement.Status.valueOf(status.toUpperCase());
                return criteriaBuilder.equal(root.get("status"), enumStatus);
            } catch (IllegalArgumentException e) {
                return criteriaBuilder.conjunction();
            }
        };
    }
}
