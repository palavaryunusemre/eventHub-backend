package com.eventhub.business.concretes;

import com.eventhub.business.abstracts.AuthService;
import com.eventhub.config.PasswordEncoderConfig;
import com.eventhub.dataAccess.InstitutionDao;
import com.eventhub.dataAccess.UserDao;
import com.eventhub.entities.Institution;
import com.eventhub.entities.User;
import com.eventhub.services.JwtService;
import com.eventhub.services.RefreshTokenService;
import com.eventhub.services.CurrentUserService;
import com.eventhub.services.AuditService;
import com.eventhub.types.enums.Role;
import com.eventhub.types.request.LogInRequest;
import com.eventhub.types.request.SignInRequest;
import com.eventhub.types.request.UpdateUserRequest;
import com.eventhub.types.request.ChangePasswordRequest;
import com.eventhub.types.response.AuthTokens;
import com.eventhub.types.response.UserResponse;
import com.eventhub.types.output.UserOutput;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthManager implements AuthService {
    private final UserDao userDao;
    private final PasswordEncoderConfig passwordEncoderConfig;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final InstitutionDao institutionDao;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Override
    public Result<String> signIn(SignInRequest request) {
        Optional<User> controlUser = userDao.findByEmail(request.getEmail());
        if (controlUser.isPresent())
            return new Result<>(false, 5005, "Bu email adresi kullanılmaktadır!");
        if (Arrays.stream(Role.values()).noneMatch(r -> r.name().equalsIgnoreCase(request.getRole())))
            return new Result<>(false, 5005, "Geçersiz rol bilgisi!");

        Institution institution = null;
        if (request.getInstitutionId() != null) {
            Optional<Institution> found = institutionDao.findById(request.getInstitutionId());
            if (found.isEmpty()) return new Result<>(false, 5005, "Geçersiz kurum bilgisi!");
            institution = found.get();
        }
        User user = User.builder()
                .email(request.getEmail().trim())
                .role(Role.valueOf(request.getRole().trim().toUpperCase(Locale.ROOT)))
                .firstName(capitalizeWords(request.getFirstName()))
                .lastName(request.getLastName().trim().toUpperCase(Locale.forLanguageTag("tr-TR")))
                .password(passwordEncoderConfig.passwordEncoder().encode(request.getPassword().trim()))
                .deleteFlag(false)
                .institution(institution)
                .institutionEmailVerified(false)
                .build();
        userDao.save(user);
        auditService.log("USER_CREATED", "USER", user.getId().toString(), user.getEmail(),
                institution != null ? institution.getId() : null);
        return new Result<>(true, 200, "Kayıt başarılı.");
    }

    @Override
    public Result<AuthTokens> logIn(LogInRequest logInRequest) {
        Optional<User> users = userDao.findByEmailAndDeleteFlag(logInRequest.getEmail(), false);
        if (users.isEmpty() || !passwordEncoderConfig.passwordEncoder()
                .matches(logInRequest.getPassword(), users.get().getPassword())) {
            return new DataResult<>(false, 5005, "Invalid credentials.");
        }
        return authenticateAndGenerateTokens(users.get());
    }

    @Override
    public Result<AuthTokens> refresh(String rawRefreshToken) {
        return refreshTokenService.rotate(rawRefreshToken)
                .map(rotated -> (Result<AuthTokens>) new DataResult<>(buildTokens(rotated.user(), rotated.rawToken()), true, 200))
                .orElse(new DataResult<>(false, 5005, "Oturum süresi doldu, lütfen tekrar giriş yapın."));
    }

    @Override
    public void logout(String rawRefreshToken) {
        refreshTokenService.findUser(rawRefreshToken).ifPresent(user -> auditService.logForUser(
                "LOGOUT", "AUTH", user.getId().toString(), "Session ended.", user));
        refreshTokenService.revoke(rawRefreshToken);
    }

    @Transactional
    @Override
    public Result<String> delete(UUID id) {
        Optional<User> user = userDao.findById(id);
        if (user.isPresent()) {
            user.get().setDeleteFlag(true);
            userDao.saveAndFlush(user.get());
            auditService.log("USER_DELETED", "USER", user.get().getId().toString(), user.get().getEmail(),
                    user.get().getInstitution() != null ? user.get().getInstitution().getId() : null);
            return new Result<>(true, 200, "Kişi kaydı silindi.");
        }
        return new Result<>(false, 5005, "Geçersiz kişi!");
    }

    @Transactional
    @Override
    public Result<String> update(UpdateUserRequest request) {
        Optional<User> targetOptional = userDao.findById(request.getId());
        if (targetOptional.isEmpty() || Boolean.TRUE.equals(targetOptional.get().getDeleteFlag())) {
            return new Result<>(false, 404, "Geçersiz kullanıcı bilgisi.");
        }
        if (Arrays.stream(Role.values()).noneMatch(role -> role.name().equalsIgnoreCase(request.getRole()))) {
            return new Result<>(false, 400, "Geçersiz rol bilgisi.");
        }

        User target = targetOptional.get();
        Role newRole = Role.valueOf(request.getRole().trim().toUpperCase(Locale.ROOT));

        Optional<User> sameEmail = userDao.findByEmail(request.getEmail().trim());
        if (sameEmail.isPresent() && !sameEmail.get().getId().equals(target.getId())) {
            return new Result<>(false, 409, "Bu e-posta adresi kullanılmaktadır.");
        }
        Institution institution = null;
        if (request.getInstitutionId() != null) {
            Optional<Institution> found = institutionDao.findById(request.getInstitutionId());
            if (found.isEmpty()) return new Result<>(false, 400, "Geçersiz kurum bilgisi.");
            institution = found.get();
        }

        target.setEmail(request.getEmail().trim());
        target.setFirstName(capitalizeWords(request.getFirstName()));
        target.setLastName(request.getLastName().trim().toUpperCase(Locale.forLanguageTag("tr-TR")));
        target.setRole(newRole);
        target.setInstitution(institution);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            target.setPassword(passwordEncoderConfig.passwordEncoder().encode(request.getPassword().trim()));
        }
        userDao.saveAndFlush(target);
        auditService.log("USER_UPDATED", "USER", target.getId().toString(), target.getEmail(),
                institution != null ? institution.getId() : null);
        return new Result<>(true, 200, "Kullanıcı bilgisi güncellendi.");
    }

    @Transactional
    @Override
    public Result<String> changePassword(ChangePasswordRequest request) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        if (currentUserId == null) return new Result<>(false, 401, "Oturum bulunamadı.");
        Optional<User> user = userDao.findById(currentUserId);
        if (user.isEmpty() || Boolean.TRUE.equals(user.get().getDeleteFlag())) {
            return new Result<>(false, 404, "Kullanıcı bulunamadı.");
        }
        if (!passwordEncoderConfig.passwordEncoder().matches(request.getCurrentPassword(), user.get().getPassword())) {
            return new Result<>(false, 400, "Mevcut parola doğru değil.");
        }
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            return new Result<>(false, 400, "Yeni parola tekrarı eşleşmiyor.");
        }
        user.get().setPassword(passwordEncoderConfig.passwordEncoder().encode(request.getNewPassword()));
        userDao.saveAndFlush(user.get());
        auditService.log("PASSWORD_CHANGED", "USER", user.get().getId().toString(), user.get().getEmail(),
                user.get().getInstitution() != null ? user.get().getInstitution().getId() : null);
        return new Result<>(true, 200, "Parolanız güncellendi.");
    }

    @Override
    public DataResult<List<UserOutput>> getUsers() {
        // ADMIN artık global bir rol — kuruma göre kapsam sınırlaması yok (eski SUPER_ADMIN/ADMIN okul bazlı modeli kaldırıldı).
        List<User> users = userDao.findAllByDeleteFlag(false);
        List<UserOutput> outputs = users.stream().map(
                user -> UserOutput.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().name())
                        .institutionId(user.getInstitution() != null ? user.getInstitution().getId() : null)
                        .createdAt(user.getCreatedAt())
                        .build()
        ).collect(Collectors.toList());
        return new DataResult<>(outputs, true, 200);
    }

    private DataResult<AuthTokens> authenticateAndGenerateTokens(User user) {
        String rawRefreshToken = refreshTokenService.issue(user);
        auditService.logForUser("LOGIN", "AUTH", user.getId().toString(), "Session started.", user);
        return new DataResult<>(buildTokens(user, rawRefreshToken), true, 200);
    }

    private AuthTokens buildTokens(User user, String rawRefreshToken) {
        UserResponse response = UserResponse.builder()
                .email(user.getEmail())
                .role(user.getRole().name())
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .institutionId(user.getInstitution() != null ? user.getInstitution().getId() : null)
                .build();
        return AuthTokens.builder()
                .authToken(jwtService.generateToken(response))
                .infoToken(jwtService.generateInfoToken(response))
                .refreshToken(rawRefreshToken)
                .build();
    }

    private String capitalizeWords(String value) {
        Locale turkish = Locale.forLanguageTag("tr-TR");
        String[] words = value.trim().toLowerCase(turkish).split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase(turkish))
                        .append(word.substring(1));
            }
            result.append(" ");
        }
        return result.toString().trim();
    }
}
