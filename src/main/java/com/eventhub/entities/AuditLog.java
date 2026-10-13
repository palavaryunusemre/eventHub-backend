package com.eventhub.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.UUID;

/**
 * Kullanıcı eylemleri için denetim kaydı (Audit Log).
 * Hangi kullanıcı, hangi okulda, ne zaman, hangi işlemi yaptığını depolar.
 * Gizlilik: şifre gibi hassas veriler {@code details} alanına yazılmaz.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "audit_logs",
       indexes = {
           @Index(name = "idx_audit_institution", columnList = "institution_id"),
           @Index(name = "idx_audit_performed_by", columnList = "performed_by_id"),
           @Index(name = "idx_audit_created_at", columnList = "created_at")
       })
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** İşlem türü — örn. USER_CREATED, DEVICE_DELETED, THRESHOLD_UPDATED */
    @Column(nullable = false, length = 64)
    private String action;

    /** Etkilenen nesne türü — örn. USER, DEVICE, THRESHOLD_PROFILE, COMPANY, MODEL */
    @Column(name = "target_type", length = 32)
    private String targetType;

    /** Etkilenen nesnenin UUID veya metinsel kimliği */
    @Column(name = "target_id", length = 128)
    private String targetId;

    /** Değişikliğin kısa açıklaması — hassas veri içermemelidir */
    @Column(columnDefinition = "TEXT")
    private String details;

    /** İşlemi gerçekleştiren kullanıcının UUID'si (token silinse bile kaydı saklar) */
    @Column(name = "performed_by_id")
    private UUID performedById;

    /** İşlemi gerçekleştiren kullanıcının o anki e-posta adresi */
    @Column(name = "performed_by_email", length = 256)
    private String performedByEmail;

    /** Kurum UUID — kuruma bağlı olmayan kullanıcı işlemlerinde null olabilir */
    @Column(name = "institution_id")
    private UUID institutionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Date createdAt;
}
