package com.eventhub.dataAccess;

import com.eventhub.entities.Registration;
import com.eventhub.types.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationDao extends JpaRepository<Registration, UUID>, JpaSpecificationExecutor<Registration> {
    @EntityGraph(attributePaths = {"event", "user"})
    Optional<Registration> findByQrToken(String qrToken);

    List<Registration> findAllByEventIdAndStatusNot(UUID eventId, RegistrationStatus status);

    List<Registration> findAllByEventIdAndUserIdAndStatus(UUID eventId, UUID userId, RegistrationStatus status);

    @EntityGraph(attributePaths = {"event", "user"})
    List<Registration> findAllByUserId(UUID userId);

    @Override
    @EntityGraph(attributePaths = {"event", "user"})
    Optional<Registration> findById(UUID id);
}
