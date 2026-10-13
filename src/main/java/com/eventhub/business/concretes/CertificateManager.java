package com.eventhub.business.concretes;

import com.eventhub.business.abstracts.CertificateService;
import com.eventhub.dataAccess.CertificateDao;
import com.eventhub.dataAccess.RegistrationDao;
import com.eventhub.entities.Certificate;
import com.eventhub.entities.Event;
import com.eventhub.entities.Registration;
import com.eventhub.services.AuditService;
import com.eventhub.services.CertificatePdfService;
import com.eventhub.services.CurrentUserService;
import com.eventhub.types.enums.EventStatus;
import com.eventhub.types.enums.RegistrationStatus;
import com.eventhub.types.output.CertificateOutput;
import com.eventhub.types.response.CertificateVerificationResponse;
import com.eventhub.utilities.result.DataResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateManager implements CertificateService {

    private final CertificateDao certificateDao;
    private final RegistrationDao registrationDao;
    private final CertificatePdfService certificatePdfService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Transactional
    @Override
    public DataResult<CertificateOutput> issue(UUID registrationId) {
        Optional<Registration> found = registrationDao.findById(registrationId);
        if (found.isEmpty()) return new DataResult<>(false, 404, "Kayıt bulunamadı.");
        Registration registration = found.get();

        Event event = registration.getEvent();
        if (event == null || event.getStatus() != EventStatus.COMPLETED) {
            return new DataResult<>(false, 409, "Sertifika yalnızca tamamlanmış etkinlikler için üretilebilir.");
        }
        if (!Boolean.TRUE.equals(event.getCertificateEnabled())) {
            return new DataResult<>(false, 409, "Bu etkinlik için sertifika üretimi etkin değil.");
        }
        if (registration.getStatus() != RegistrationStatus.CHECKED_IN) {
            return new DataResult<>(false, 409, "Sertifika yalnızca check-in yapmış katılımcılar için üretilebilir.");
        }

        Optional<Certificate> existing = certificateDao.findByRegistrationId(registrationId);
        if (existing.isPresent()) {
            return new DataResult<>(toOutput(existing.get()), true, 200);
        }

        String code = UUID.randomUUID().toString().replace("-", "");
        String attendeeName = registration.getUser() != null
                ? registration.getUser().getFirstName() + " " + registration.getUser().getLastName()
                : "";
        String verificationUrl = "/verify/" + code;

        String pdfPath = null;
        try {
            pdfPath = certificatePdfService.renderAndStore(code, attendeeName, event.getTitle(),
                    event.getStartAt(), new Date(), verificationUrl);
        } catch (Exception e) {
            log.error("Sertifika PDF üretilemedi [registrationId={}]: {}", registrationId, e.getMessage());
        }

        Certificate certificate = Certificate.builder()
                .id(UUID.randomUUID())
                .registration(registration)
                .code(code)
                .pdfPath(pdfPath)
                .revoked(false)
                .build();
        certificateDao.save(certificate);
        auditService.log("CERTIFICATE_ISSUED", "CERTIFICATE", certificate.getId().toString(), code, null);

        return new DataResult<>(toOutput(certificate), true, 200);
    }

    @Override
    public DataResult<List<CertificateOutput>> getMine() {
        UUID userId = currentUserService.getCurrentUserId();
        List<Certificate> certificates = certificateDao.findAllByRegistrationUserId(userId);
        return new DataResult<>(certificates.stream().map(this::toOutput).collect(Collectors.toList()), true, 200);
    }

    @Override
    public CertificateVerificationResponse verifyByCode(String code) {
        Optional<Certificate> found = certificateDao.findByCode(code);
        if (found.isEmpty() || Boolean.TRUE.equals(found.get().getRevoked())) {
            return CertificateVerificationResponse.builder()
                    .valid(false)
                    .code(code)
                    .message("Sertifika bulunamadı veya geçersiz.")
                    .build();
        }
        Certificate certificate = found.get();
        Registration registration = certificate.getRegistration();
        Event event = registration != null ? registration.getEvent() : null;
        String attendeeName = registration != null && registration.getUser() != null
                ? registration.getUser().getFirstName() + " " + registration.getUser().getLastName()
                : null;
        return CertificateVerificationResponse.builder()
                .valid(true)
                .code(certificate.getCode())
                .eventTitle(event != null ? event.getTitle() : null)
                .attendeeName(attendeeName)
                .issuedAt(certificate.getIssuedAt())
                .eventStartAt(event != null ? event.getStartAt() : null)
                .eventEndAt(event != null ? event.getEndAt() : null)
                .message("Sertifika geçerli.")
                .build();
    }

    @Override
    public byte[] getPdfBytes(String code) {
        Optional<Certificate> found = certificateDao.findByCode(code);
        if (found.isEmpty() || Boolean.TRUE.equals(found.get().getRevoked())) return null;
        String pdfPath = found.get().getPdfPath();
        if (pdfPath == null) return null;
        try {
            return java.nio.file.Files.readAllBytes(java.nio.file.Path.of(pdfPath));
        } catch (java.io.IOException e) {
            log.error("Sertifika PDF'i okunamadı [code={}]: {}", code, e.getMessage());
            return null;
        }
    }

    private CertificateOutput toOutput(Certificate c) {
        Registration registration = c.getRegistration();
        Event event = registration != null ? registration.getEvent() : null;
        return CertificateOutput.builder()
                .id(c.getId())
                .registrationId(registration != null ? registration.getId() : null)
                .eventId(event != null ? event.getId() : null)
                .eventTitle(event != null ? event.getTitle() : null)
                .code(c.getCode())
                .pdfPath(c.getPdfPath())
                .issuedAt(c.getIssuedAt())
                .revoked(c.getRevoked())
                .build();
    }
}
