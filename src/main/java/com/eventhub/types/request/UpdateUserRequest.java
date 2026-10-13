package com.eventhub.types.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateUserRequest {
    @NotNull(message = "Kullanıcı kimliği gerekli")
    private UUID id;

    @NotBlank(message = "E-posta adresi boş bırakılamaz")
    @Email(message = "Geçersiz e-posta formatı")
    private String email;

    @NotBlank(message = "Ad alanı boş bırakılamaz")
    @Size(max = 50, message = "Ad uzunluğu en fazla 50 karakter olabilir")
    private String firstName;

    @NotBlank(message = "Soyadı alanı boş bırakılamaz")
    @Size(max = 50, message = "Soyadı uzunluğu en fazla 50 karakter olabilir")
    private String lastName;

    @Size(min = 8, max = 16, message = "Parola uzunluğu 8-16 karakter arasında olmalıdır")
    private String password;

    @NotBlank(message = "Rol seçilmeli")
    private String role;

    private UUID institutionId;
}
