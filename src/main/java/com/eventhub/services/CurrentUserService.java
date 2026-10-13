package com.eventhub.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.eventhub.dataAccess.UserDao;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final JwtService jwtService;
    private final HttpServletRequest request;
    private final UserDao userDao;

    public String getToken() {
        // Önce SecurityContext'ten almayı dene
        String token = jwtService.getTokenFromContext();
        if (token != null) {
            return token;
        }
        // SecurityContext'ten alınamadıysa header'dan al
        String authHeader = request.getHeader("Authorization");
        return jwtService.extractTokenFromHeader(authHeader);
    }

    public UUID getCurrentUserId() {
        return jwtService.getCurrentUserId();
    }

    public String getCurrentUserRole() {
        return jwtService.getCurrentUserRole();
    }

    public boolean isAdmin() {
        return "ADMIN".equals(getCurrentUserRole());
    }

    public UUID getCurrentUserInstitutionId() {
        UUID institutionId = jwtService.getCurrentUserInstitutionId();
        if (institutionId != null) return institutionId;

        // Eski oturum token'larında institutionId claim'i bulunmayabilir. Kullanıcının
        // güncel kurumunu DB'den okuyarak kapsamı yine doğru uygula.
        UUID userId = getCurrentUserId();
        if (userId == null) return null;
        return userDao.findById(userId)
                .map(user -> user.getInstitution() == null ? null : user.getInstitution().getId())
                .orElse(null);
    }
}
