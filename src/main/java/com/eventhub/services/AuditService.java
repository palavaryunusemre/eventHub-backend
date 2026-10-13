package com.eventhub.services;

import com.eventhub.dataAccess.AuditLogDao;
import com.eventhub.entities.AuditLog;
import com.eventhub.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogDao auditLogDao;
    private final JwtService jwtService;

    /** Writes a record for the authenticated request user. */
    public void log(String action, String targetType, String targetId,
                    String details, UUID institutionId) {
        UUID userId = null;
        String userEmail = null;
        try {
            userId = jwtService.getCurrentUserId();
            userEmail = jwtService.getCurrentUserEmail();
        } catch (Exception ignored) {
            // System-originated records may not have an authenticated user.
        }
        write(action, targetType, targetId, details, institutionId, userId, userEmail);
    }

    /** Used by authentication flows before a SecurityContext exists. */
    public void logForUser(String action, String targetType, String targetId,
                           String details, User user) {
        UUID institutionId = user.getInstitution() == null ? null : user.getInstitution().getId();
        write(action, targetType, targetId, details, institutionId, user.getId(), user.getEmail());
    }

    private void write(String action, String targetType, String targetId,
                       String details, UUID institutionId, UUID userId, String userEmail) {
        try {
            auditLogDao.save(AuditLog.builder()
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .details(details)
                    .performedById(userId)
                    .performedByEmail(userEmail)
                    .institutionId(institutionId)
                    .build());
        } catch (Exception e) {
            log.error("Audit record could not be saved [action={}]: {}", action, e.getMessage());
        }
    }
}
