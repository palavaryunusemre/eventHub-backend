package com.eventhub.entities;

import com.eventhub.types.enums.EventFormat;
import com.eventhub.types.enums.EventStatus;
import com.eventhub.types.enums.EventVisibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, unique = true)
    private String slug;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "cover_image_url")
    private String coverImageUrl;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category")
    private Category category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer", nullable = false)
    private User organizer;
    @Enumerated(EnumType.STRING)
    private EventVisibility visibility;
    /** visibility = INSTITUTION_ONLY ise etkinliğin görünür olduğu kurumlar. */
    @Builder.Default
    @ManyToMany
    @JoinTable(name = "event_allowed_institutions",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "institution_id"))
    private Set<Institution> allowedInstitutions = new HashSet<>();
    @Enumerated(EnumType.STRING)
    private EventFormat format;
    private String location;
    @Column(name = "online_url")
    private String onlineUrl;
    @Column(name = "start_at", nullable = false)
    private Date startAt;
    @Column(name = "end_at", nullable = false)
    private Date endAt;
    private Integer capacity;
    @Enumerated(EnumType.STRING)
    private EventStatus status;
    @Builder.Default
    @Column(name = "certificate_enabled")
    private Boolean certificateEnabled = false;
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    @Column(name = "reviewed_at")
    private Date reviewedAt;
    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;
}
