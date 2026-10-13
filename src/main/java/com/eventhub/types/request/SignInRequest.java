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

/** İsme rağmen "sign up" (kayıt) akışı içindir — mevcut isimlendirme korunmuştur. */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SignInRequest {
    @NotBlank(message = "E-mail adresi boş bırakılamaz")
    @Email(message = "Geçersiz e-mail formatı")
    private String email;

    @NotBlank(message = "Ad alanı boş bırakılamaz")
    @Size(max = 50, message = "Ad uzunluğu en fazla 50 karakter olabilir")
    private String firstName;

    @NotBlank(message = "Soyadı boş bırakılamaz")
    @Size(max = 50, message = "Soyadı uzunluğu en fazla 50 karakter olabilir")
    private String lastName;

    @NotBlank(message = "Parola boş bırakılamaz")
    @Size(min = 8, max = 16, message = "Parola uzunluğu 8-16 karakter arasında olmalıdır")
    private String password;
    @NotBlank(message = "Rol seçilmeli!")
    @NotNull(message = "Rol seçilmeli!")
    private String role;
    /** Opsiyonel — kullanıcı bir kuruma bağlıysa doldurulur. */
    private UUID institutionId;
}
