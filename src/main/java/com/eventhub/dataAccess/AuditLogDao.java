package com.eventhub.dataAccess;

import com.eventhub.entities.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Date;
import java.util.UUID;

public interface AuditLogDao extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByInstitutionIdOrderByCreatedAtDesc(UUID institutionId, Pageable pageable);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByInstitutionIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID institutionId, Date from, Date to, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            Date from, Date to, Pageable pageable);
}
