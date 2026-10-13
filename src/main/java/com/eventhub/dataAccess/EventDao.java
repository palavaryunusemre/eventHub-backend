package com.eventhub.dataAccess;

import com.eventhub.entities.Event;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventDao extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {
    @Override
    @EntityGraph(attributePaths = {"category", "organizer", "allowedInstitutions"})
    Optional<Event> findById(UUID id);

    Optional<Event> findBySlug(String slug);

    List<Event> findAllByOrganizerId(UUID organizerId);
}
