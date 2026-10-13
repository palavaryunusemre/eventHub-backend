package com.eventhub.dataAccess;

import com.eventhub.entities.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CheckInDao extends JpaRepository<CheckIn, UUID> {
    Optional<CheckIn> findByRegistrationId(UUID registrationId);
}
