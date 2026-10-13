package com.eventhub.types.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login sırasında üretilen iki token'ı taşır — controller bunları
 * Set-Cookie header'larına yazar, JSON response body'sine ASLA koymaz
 * (authToken HttpOnly cookie dışında hiçbir yerde JS'e görünmemeli).
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AuthTokens {
    private String authToken;
    private String infoToken;
    private String refreshToken;
}
