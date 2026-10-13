package com.eventhub.dataAccess;

import com.eventhub.entities.Certificate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateDao extends JpaRepository<Certificate, UUID> {
    @EntityGraph(attributePaths = {"registration", "registration.event", "registration.user"})
    Optional<Certificate> findByCode(String code);

    Optional<Certificate> findByRegistrationId(UUID registrationId);

    @EntityGraph(attributePaths = {"registration", "registration.event"})
    List<Certificate> findAllByRegistrationUserId(UUID userId);
}
