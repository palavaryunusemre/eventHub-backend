package com.eventhub.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kurum e-posta doğrulaması (örn. ogrenci.xxx.edu.tr) için bağımsız bir bileşen.
 * NOT: Bilinçli olarak {@code UserManager}/{@code InstitutionManager} içinden
 * çağrılmıyor — ilk aşamada kendi başına çalışan, ileride kayıt akışına
 * bağlanabilecek bağımsız bir servis olarak tasarlandı (bkz. plan belgesi).
 * Token'lar şimdilik in-memory tutuluyor; üretim ortamında Redis/DB'ye taşınmalı.
 */
@Slf4j
@Service
public class EmailVerificationService {

    private static final long TOKEN_TTL_MS = 24L * 60 * 60 * 1000; // 24 saat

    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, PendingVerification> tokens = new ConcurrentHashMap<>();

    @Value("${spring.mail.username:no-reply@eventhub.local}")
    private String fromAddress;

    public EmailVerificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Verilen e-posta için yeni bir doğrulama token'ı üretir ve geçici olarak saklar. */
    public String generateVerificationToken(String email) {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, new PendingVerification(email, System.currentTimeMillis() + TOKEN_TTL_MS));
        return token;
    }

    /** Token'ı doğrular; geçerliyse e-posta adresini döner, aksi halde null. */
    public String verifyToken(String token) {
        PendingVerification pending = tokens.get(token);
        if (pending == null) return null;
        if (pending.expiresAtMs < System.currentTimeMillis()) {
            tokens.remove(token);
            return null;
        }
        tokens.remove(token);
        return pending.email;
    }

    /** Doğrulama e-postasını gönderir — SMTP yapılandırılmamışsa hatayı loglar, exception fırlatmaz. */
    public void sendVerificationEmail(String email, String token, String verificationBaseUrl) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(email);
            message.setSubject("EventHub kurum e-postanızı doğrulayın");
            message.setText("Kurum e-postanızı doğrulamak için bağlantıya tıklayın: "
                    + verificationBaseUrl + "?token=" + token);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Doğrulama e-postası gönderilemedi [email={}]: {}", email, e.getMessage());
        }
    }

    private record PendingVerification(String email, long expiresAtMs) {}
}
