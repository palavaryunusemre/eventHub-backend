package com.eventhub.types.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class EventOutput {
    private UUID id;
    private String title;
    private String slug;
    private String description;
    private String coverImageUrl;
    private UUID categoryId;
    private String categoryName;
    private UUID organizerId;
    private String organizerName;
    private String visibility;
    private Set<UUID> allowedInstitutionIds;
    private String format;
    private String location;
    private String onlineUrl;
    private Date startAt;
    private Date endAt;
    private Integer capacity;
    private int registeredCount;
    private String status;
    private Boolean certificateEnabled;
    private String rejectionReason;
    private UUID reviewedById;
    private Date reviewedAt;
    private Date createdAt;
    private Date updatedAt;
}
