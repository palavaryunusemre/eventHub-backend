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
 * Refresh token'lar ham haliyle DB'de saklanmaz — yalnızca SHA-256 hash'i
 * ({@link #tokenHash}) tutulur (bkz. RefreshTokenService), tıpkı parola
 * sıfırlama token'larında olduğu gibi. Her refresh çağrısında token
 * rotate edilir: mevcut satır revoked=true yapılır, yeni bir satır açılır —
 * çalınmış bir refresh token'ın tekrar kullanılmasını (replay) engeller.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "refresh_token")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users", nullable = false)
    private User user;
    @Column(name = "expires_at", nullable = false)
    private Date expiresAt;
    @Builder.Default
    @Column(nullable = false)
    private Boolean revoked = false;
    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;
}
