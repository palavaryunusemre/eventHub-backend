package com.eventhub.dataAccess;

import com.eventhub.entities.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InstitutionDao extends JpaRepository<Institution, UUID> {
    List<Institution> findAllByDeleteFlag(boolean deleteFlag);
}
