package com.eventhub.business.concretes;

import com.eventhub.business.abstracts.RegistrationService;
import com.eventhub.dataAccess.EventDao;
import com.eventhub.dataAccess.RegistrationDao;
import com.eventhub.dataAccess.UserDao;
import com.eventhub.entities.Event;
import com.eventhub.entities.Institution;
import com.eventhub.entities.Registration;
import com.eventhub.entities.User;
import com.eventhub.services.AuditService;
import com.eventhub.services.CurrentUserService;
import com.eventhub.types.enums.EventStatus;
import com.eventhub.types.enums.EventVisibility;
import com.eventhub.types.enums.RegistrationStatus;
import com.eventhub.types.output.RegistrationOutput;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationManager implements RegistrationService {

    private final RegistrationDao registrationDao;
    private final EventDao eventDao;
    private final UserDao userDao;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Transactional
    @Override
    public Result<String> register(UUID eventId) {
        UUID userId = currentUserService.getCurrentUserId();
        Optional<User> userOptional = userDao.findById(userId);
        if (userOptional.isEmpty()) return new Result<>(false, 401, "Oturum bulunamadı.");
        User user = userOptional.get();

        Optional<Event> eventOptional = eventDao.findById(eventId);
        if (eventOptional.isEmpty()) return new Result<>(false, 404, "Etkinlik bulunamadı.");
        Event event = eventOptional.get();

        if (event.getStatus() != EventStatus.PUBLISHED) {
            return new Result<>(false, 409, "Bu etkinlik için kayıt açık değil.");
        }

        if (event.getVisibility() == EventVisibility.INSTITUTION_ONLY) {
            boolean allowed = user.getInstitution() != null && event.getAllowedInstitutions() != null
                    && event.getAllowedInstitutions().stream()
                        .map(Institution::getId)
                        .anyMatch(id -> id.equals(user.getInstitution().getId()));
            if (!allowed) {
                return new Result<>(false, 403, "Bu etkinlik kurumunuza açık değil.");
            }
        }

        boolean alreadyRegistered = !registrationDao
                .findAllByEventIdAndUserIdAndStatus(eventId, userId, RegistrationStatus.REGISTERED).isEmpty()
                || !registrationDao.findAllByEventIdAndUserIdAndStatus(eventId, userId, RegistrationStatus.CHECKED_IN).isEmpty();
        if (alreadyRegistered) {
            return new Result<>(false, 409, "Bu etkinliğe zaten kayıtlısınız.");
        }

        if (event.getCapacity() != null) {
            long activeCount = registrationDao.findAllByEventIdAndStatusNot(eventId, RegistrationStatus.CANCELLED).size();
            if (activeCount >= event.getCapacity()) {
                return new Result<>(false, 409, "Etkinlik kapasitesi dolu.");
            }
        }

        Registration registration = Registration.builder()
                .id(UUID.randomUUID())
                .event(event)
                .user(user)
                .status(RegistrationStatus.REGISTERED)
                .qrToken(UUID.randomUUID().toString())
                .build();
        registrationDao.save(registration);
        auditService.log("REGISTRATION_CREATED", "REGISTRATION", registration.getId().toString(),
                "Event: " + event.getTitle(), null);
        return new Result<>(true, 200, "Kayıt başarılı.");
    }

    @Transactional
    @Override
    public Result<String> cancel(UUID registrationId) {
        Optional<Registration> found = registrationDao.findById(registrationId);
        if (found.isEmpty()) return new Result<>(false, 404, "Kayıt bulunamadı.");
        Registration registration = found.get();
        UUID userId = currentUserService.getCurrentUserId();
        if (registration.getUser() == null || !registration.getUser().getId().equals(userId)) {
            return new Result<>(false, 403, "Bu kaydı iptal etme yetkiniz yok.");
        }
        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            return new Result<>(false, 409, "Kayıt zaten iptal edilmiş.");
        }
        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationDao.saveAndFlush(registration);
        auditService.log("REGISTRATION_CANCELLED", "REGISTRATION", registration.getId().toString(), null, null);
        return new Result<>(true, 200, "Kayıt iptal edildi.");
    }

    @Override
    public DataResult<List<RegistrationOutput>> getMine() {
        UUID userId = currentUserService.getCurrentUserId();
        List<Registration> registrations = registrationDao.findAllByUserId(userId);
        return new DataResult<>(registrations.stream().map(this::toOutput).collect(Collectors.toList()), true, 200);
    }

    private RegistrationOutput toOutput(Registration r) {
        return RegistrationOutput.builder()
                .id(r.getId())
                .eventId(r.getEvent() != null ? r.getEvent().getId() : null)
                .eventTitle(r.getEvent() != null ? r.getEvent().getTitle() : null)
                .eventStartAt(r.getEvent() != null ? r.getEvent().getStartAt() : null)
                .eventEndAt(r.getEvent() != null ? r.getEvent().getEndAt() : null)
                .userId(r.getUser() != null ? r.getUser().getId() : null)
                .userEmail(r.getUser() != null ? r.getUser().getEmail() : null)
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .qrToken(r.getQrToken())
                .registeredAt(r.getRegisteredAt())
                .checkedInAt(r.getCheckedInAt())
                .build();
    }
}
