package com.eventhub.services;

import com.eventhub.dataAccess.TokenDao;
import com.eventhub.types.response.UserResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {
    public static final String COOKIE_NAME = "eventhub_jwt";
    public static final String INFO_COOKIE_NAME = "eventhub_user";
    public static final String REFRESH_COOKIE_NAME = "eventhub_refresh";
    public static final String REFRESH_COOKIE_PATH = "/api/auth";
    private static final long EXPIRATION_MS = 15 * 60 * 1000;
    private static final long REFRESH_EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000;

    @Value("${spring.security.jwt.secret-key}")
    private String SECRET_KEY;
    @Value("${spring.security.jwt.cookie-secure:false}")
    private boolean COOKIE_SECURE;

    private final TokenDao tokenDao;

    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
    public String findEmail(String token) {
        return exportToken(token, Claims::getSubject);
    }

    private <T> T exportToken(String token, Function<Claims,T> claimsTFunction) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsTFunction.apply(claims);
    }

    private Key getKey() {
        byte[] key = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(key);
    }

    public boolean tokenControl(String jwt, UserDetails userDetails) {
        final String email = findEmail(jwt);
        return(email.equals(userDetails.getUsername()) && !exportToken(jwt,Claims::getExpiration).before(new Date()) && tokenDao.findByToken(jwt).isEmpty());

    }

    /** Backend'in her istekte doğruladığı asıl kimlik token'ı (Authorization header ya da HttpOnly cookie). */
    public String generateToken(UserResponse user) {
        return buildJwt(user);
    }

    /** Sadece görüntüleme amaçlı — frontend'in email/ad-soyad/rol okuması için ayrı, kimlik doğrulamada KULLANILMAZ. */
    public String generateInfoToken(UserResponse user) {
        return buildJwt(user);
    }

    private String buildJwt(UserResponse user) {
        return Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject(user.getEmail())
                .claim("userID",user.getId())
                .claim("email",user.getEmail())
                .claim("role","ROLE_"+user.getRole())
                .claim("firstName",user.getFirstName())
                .claim("lastName",user.getLastName())
                .claim("institutionId", user.getInstitutionId())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis()+EXPIRATION_MS))
                .setId(UUID.randomUUID().toString())
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Login sonrası tarayıcıya yazılacak HttpOnly auth cookie'si — JS erişemez. */
    public ResponseCookie buildAuthCookie(String token) {
        return cookie(COOKIE_NAME, token, true, Duration.ofMillis(EXPIRATION_MS));
    }

    /** Login sonrası tarayıcıya yazılacak, JS'in okuyabileceği kullanıcı bilgisi cookie'si. */
    public ResponseCookie buildInfoCookie(String token) {
        return cookie(INFO_COOKIE_NAME, token, false, Duration.ofMillis(EXPIRATION_MS));
    }

    /** Refresh cookie'si — sadece /api/auth path'ine gönderilir, HttpOnly. */
    public ResponseCookie buildRefreshCookie(String rawRefreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, rawRefreshToken)
                .httpOnly(true)
                .secure(COOKIE_SECURE)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ofMillis(REFRESH_EXPIRATION_MS))
                .build();
    }

    /** Logout'ta üç cookie'yi de tarayıcıdan silmek için (Max-Age=0). */
    public ResponseCookie clearAuthCookie() {
        return cookie(COOKIE_NAME, "", true, Duration.ZERO);
    }

    public ResponseCookie clearInfoCookie() {
        return cookie(INFO_COOKIE_NAME, "", false, Duration.ZERO);
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(COOKIE_SECURE)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie cookie(String name, String value, boolean httpOnly, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(COOKIE_SECURE)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
    public String getCurrentUserRole() {
        String token = getTokenFromContext();
        if (token != null) {
            String roleWithPrefix = exportToken(token, claims -> claims.get("role", String.class));
            return roleWithPrefix != null ? roleWithPrefix.replace("ROLE_", "") : null;
        }
        return null;
    }

    public UUID getCurrentUserId() {
        String token = getTokenFromContext();
        if (token != null) {
            Object userIdObj = exportToken(token, claims -> claims.get("userID"));
            if (userIdObj != null) {
                return UUID.fromString(userIdObj.toString());
            }
        }
        return null;
    }

    public UUID getCurrentUserInstitutionId() {
        String token = getTokenFromContext();
        if (token != null) {
            Object institutionIdObj = exportToken(token, claims -> claims.get("institutionId"));
            if (institutionIdObj != null) {
                return UUID.fromString(institutionIdObj.toString());
            }
        }
        return null;
    }

    public String getCurrentUserEmail() {
        String token = getTokenFromContext();
        return token != null ? findEmail(token) : null;
    }

    public String getTokenFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return (String) authentication.getCredentials();
        }
        return null;
    }
}
