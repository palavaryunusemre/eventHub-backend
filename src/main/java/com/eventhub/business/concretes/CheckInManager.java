package com.eventhub.business.concretes;

import com.eventhub.business.abstracts.CheckInService;
import com.eventhub.dataAccess.CheckInDao;
import com.eventhub.dataAccess.RegistrationDao;
import com.eventhub.dataAccess.UserDao;
import com.eventhub.entities.CheckIn;
import com.eventhub.entities.Event;
import com.eventhub.entities.Registration;
import com.eventhub.entities.User;
import com.eventhub.services.AuditService;
import com.eventhub.services.CurrentUserService;
import com.eventhub.types.enums.RegistrationStatus;
import com.eventhub.types.output.CheckInOutput;
import com.eventhub.utilities.result.DataResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CheckInManager implements CheckInService {

    /** Etkinlik başlangıcından önce/sonundan sonra check-in'e izin verilen tolerans penceresi. */
    private static final long WINDOW_MS = 60L * 60 * 1000; // 1 saat

    private final CheckInDao checkInDao;
    private final RegistrationDao registrationDao;
    private final UserDao userDao;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Transactional
    @Override
    public DataResult<CheckInOutput> scan(String qrToken) {
        Optional<Registration> found = registrationDao.findByQrToken(qrToken);
        if (found.isEmpty()) {
            return new DataResult<>(false, 404, "Geçersiz QR kod.");
        }
        Registration registration = found.get();

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            return new DataResult<>(false, 409, "Bu kayıt iptal edilmiş.");
        }
        if (registration.getStatus() == RegistrationStatus.CHECKED_IN) {
            return new DataResult<>(false, 409, "Bu QR kod zaten kullanıldı.");
        }

        Event event = registration.getEvent();
        Date now = new Date();
        if (event != null && event.getStartAt() != null
                && now.before(new Date(event.getStartAt().getTime() - WINDOW_MS))) {
            return new DataResult<>(false, 409, "Etkinlik henüz başlamadı.");
        }
        if (event != null && event.getEndAt() != null
                && now.after(new Date(event.getEndAt().getTime() + WINDOW_MS))) {
            return new DataResult<>(false, 409, "Etkinlik check-in süresi doldu.");
        }

        registration.setStatus(RegistrationStatus.CHECKED_IN);
        registration.setCheckedInAt(now);
        registrationDao.saveAndFlush(registration);

        User scannedBy = userDao.findById(currentUserService.getCurrentUserId()).orElse(null);
        CheckIn checkIn = CheckIn.builder()
                .registration(registration)
                .scannedBy(scannedBy)
                .build();
        checkInDao.save(checkIn);

        auditService.log("CHECKED_IN", "REGISTRATION", registration.getId().toString(), null, null);

        return new DataResult<>(toOutput(checkIn, registration), true, 200);
    }

    private CheckInOutput toOutput(CheckIn checkIn, Registration registration) {
        return CheckInOutput.builder()
                .id(checkIn.getId())
                .registrationId(registration.getId())
                .eventId(registration.getEvent() != null ? registration.getEvent().getId() : null)
                .eventTitle(registration.getEvent() != null ? registration.getEvent().getTitle() : null)
                .userId(registration.getUser() != null ? registration.getUser().getId() : null)
                .userEmail(registration.getUser() != null ? registration.getUser().getEmail() : null)
                .userName(registration.getUser() != null
                        ? (registration.getUser().getFirstName() + " " + registration.getUser().getLastName())
                        : null)
                .scannedAt(checkIn.getScannedAt())
                .scannedById(checkIn.getScannedBy() != null ? checkIn.getScannedBy().getId() : null)
                .build();
    }
}
