package com.eventhub.dataAccess.spefications;

import com.eventhub.entities.Event;
import com.eventhub.types.enums.EventStatus;
import com.eventhub.types.enums.EventVisibility;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventSpecification {

    /**
     * Herkese açık keşif (discovery) listesi için görünürlük filtresi:
     * PUBLIC + PUBLISHED her zaman görünür; INSTITUTION_ONLY + PUBLISHED yalnızca
     * kullanıcının kurumu allowedInstitutions içinde ise görünür.
     */
    public static Specification<Event> visibleTo(UUID currentUserInstitutionId, String search, UUID categoryId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), EventStatus.PUBLISHED));

            Predicate isPublic = cb.equal(root.get("visibility"), EventVisibility.PUBLIC);
            Predicate isVisibleToInstitution;
            if (currentUserInstitutionId != null) {
                Join<Object, Object> allowed = root.join("allowedInstitutions", JoinType.LEFT);
                isVisibleToInstitution = cb.and(
                        cb.equal(root.get("visibility"), EventVisibility.INSTITUTION_ONLY),
                        cb.equal(allowed.get("id"), currentUserInstitutionId));
            } else {
                isVisibleToInstitution = cb.disjunction();
            }
            predicates.add(cb.or(isPublic, isVisibleToInstitution));

            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + search.trim().toLowerCase() + "%"));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (query != null) {
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Event> byOrganizer(UUID organizerId) {
        return (root, query, cb) -> cb.equal(root.get("organizer").get("id"), organizerId);
    }

    public static Specification<Event> byStatus(EventStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
