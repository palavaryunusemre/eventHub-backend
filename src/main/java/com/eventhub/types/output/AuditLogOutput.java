package com.eventhub.types.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AuditLogOutput {
    private UUID id;
    private String action;
    private String targetType;
    private String targetId;
    private String details;
    private UUID performedById;
    private String performedByEmail;
    private UUID institutionId;
    private Date createdAt;
}
