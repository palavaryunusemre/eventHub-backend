package com.eventhub.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration", nullable = false, unique = true)
    private Registration registration;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(name = "pdf_path")
    private String pdfPath;
    @CreationTimestamp
    @Column(name = "issued_at", updatable = false)
    private Date issuedAt;
    @Builder.Default
    private Boolean revoked = false;
}
