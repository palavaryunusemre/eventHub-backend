package com.eventhub.entities;

import com.eventhub.types.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
// User, hem JPA entity'si hem Spring Security Principal'ı olarak kullanılıyor — bkz. eski Users.java'daki not,
// equals/hashCode sadece id üzerinden karşılaştırılıyor ki lazy "institution" ilişkisi LazyInitializationException'a yol açmasın.
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
// "user" Postgres'te ayrılmış (reserved) bir kelime — Hibernate ALTER TABLE
// DDL'lerinde bunu tırnaksız ürettiği için sütun/FK eklerken syntax error alınıyordu.
// Tablo adını açıkça "users" yaparak bu çakışmayı tamamen ortadan kaldırıyoruz.
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    @Email
    @NotNull
    @Column(unique = true)
    private String email;
    @NotNull
    @NotBlank
    private String firstName;
    @NotNull
    @NotBlank
    private String lastName;
    @NotNull
    @NotBlank
    private String password;
    @Enumerated(EnumType.STRING)
    private Role role;
    private Boolean deleteFlag;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution")
    private Institution institution;
    @Builder.Default
    @Column(name = "institution_email_verified")
    private Boolean institutionEmailVerified = false;
    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
