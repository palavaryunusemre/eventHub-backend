package com.eventhub.services;

import com.eventhub.dataAccess.RefreshTokenDao;
import com.eventhub.entities.RefreshToken;
import com.eventhub.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/**
 * Opak (JWT olmayan, rastgele üretilmiş) refresh token'ların yaşam döngüsünü
 * yönetir. Access/info JWT'leri kısa ömürlü (bkz. JwtService) olduğu için
 * kullanıcı oturumu bu token sayesinde /api/auth/refresh ile sessizce
 * uzatılır — DB'de sadece hash tutulur, ham token yalnızca istemciye
 * (HttpOnly cookie olarak) bir kez gönderilir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final long REFRESH_EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000; // 7 gün
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenDao refreshTokenDao;
    private final SecureRandom secureRandom = new SecureRandom();

    /** Login sonrası kullanıcı için yeni bir refresh token üretir ve hash'ini kalıcı hale getirir. */
    @Transactional
    public String issue(User user) {
        String rawToken = generateOpaqueToken();
        RefreshToken entity = RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .expiresAt(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_MS))
                .revoked(false)
                .build();
        refreshTokenDao.save(entity);
        return rawToken;
    }

    /**
     * Gelen refresh token'ı doğrular; geçerliyse eskisini iptal edip (rotation)
     * aynı kullanıcı için yeni bir refresh token üretir. Geçersiz/süresi
     * dolmuş/iptal edilmiş bir token'la çağrılırsa boş sonuç döner.
     */
    @Transactional
    public Optional<Rotated> rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();

        Optional<RefreshToken> found = refreshTokenDao.findByTokenHash(hash(rawToken));
        if (found.isEmpty()) return Optional.empty();

        RefreshToken existing = found.get();
        if (Boolean.TRUE.equals(existing.getRevoked()) || existing.getExpiresAt().before(new Date())) {
            // Süresi dolmuş/iptal edilmiş bir token tekrar sunuluyorsa (olası çalıntı token
            // işareti) o kullanıcının tüm refresh token'larına dokunmuyoruz şimdilik —
            // yalnızca isteği reddediyoruz.
            return Optional.empty();
        }

        existing.setRevoked(true);
        refreshTokenDao.save(existing);

        User user = existing.getUser();
        String newRawToken = issue(user);
        return Optional.of(new Rotated(user, newRawToken));
    }

    /** Logout sırasında verilen refresh token'ı iptal eder — bulunamazsa sessizce geçer. */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokenDao.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenDao.save(token);
        });
    }

    /** Returns the refresh token owner for the logout audit record. */
    @Transactional(readOnly = true)
    public Optional<User> findUser(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        return refreshTokenDao.findByTokenHash(hash(rawToken)).map(RefreshToken::getUser);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 kullanılamıyor", e);
        }
    }

    public record Rotated(User user, String rawToken) {}
}
