package com.eventhub.dataAccess;

import com.eventhub.entities.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDao extends JpaRepository<User, UUID> {
    @EntityGraph(attributePaths = "institution")
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "institution")
    Optional<User> findByEmailAndDeleteFlag(String email, boolean deleteFlag);

    @Override
    @EntityGraph(attributePaths = "institution")
    Optional<User> findById(UUID id);

    List<User> findAllByDeleteFlag(boolean deleteFlag);

    List<User> findAllByDeleteFlagAndInstitutionId(boolean deleteFlag, UUID institutionId);
}
