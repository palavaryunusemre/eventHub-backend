package com.eventhub.dataAccess.spefications;

import com.eventhub.entities.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AuditLogSpecification {

    public static Specification<AuditLog> filter(
            UUID institutionId,
            Date from,
            Date to,
            String action,
            String targetType,
            String email) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (institutionId != null) {
                predicates.add(cb.equal(root.get("institutionId"), institutionId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("action")),
                        "%" + action.trim().toLowerCase() + "%"));
            }
            if (targetType != null && !targetType.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("targetType")),
                        "%" + targetType.trim().toLowerCase() + "%"));
            }
            if (email != null && !email.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("performedByEmail")),
                        "%" + email.trim().toLowerCase() + "%"));
            }

            if (query != null) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
