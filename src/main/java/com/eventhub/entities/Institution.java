package com.eventhub.entities;

import com.eventhub.types.enums.InstitutionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table
@Entity
public class Institution {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    private String name;
    /** Kurum e-posta domaini, örn. ogrenci.xxx.edu.tr — kurum e-posta doğrulaması için kullanılır. */
    @Column(name = "email_domain")
    private String emailDomain;
    @Enumerated(EnumType.STRING)
    private InstitutionType type;
    @Builder.Default
    private Boolean verified = false;
    private Boolean deleteFlag;
    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;
    @OneToMany(mappedBy = "institution")
    private Set<User> users;
}
