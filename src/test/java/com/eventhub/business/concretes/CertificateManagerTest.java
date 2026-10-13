package com.eventhub.business.concretes;

import com.eventhub.dataAccess.CertificateDao;
import com.eventhub.dataAccess.RegistrationDao;
import com.eventhub.entities.Certificate;
import com.eventhub.entities.Event;
import com.eventhub.entities.Registration;
import com.eventhub.entities.User;
import com.eventhub.services.AuditService;
import com.eventhub.services.CertificatePdfService;
import com.eventhub.services.CurrentUserService;
import com.eventhub.types.enums.EventStatus;
import com.eventhub.types.enums.RegistrationStatus;
import com.eventhub.types.output.CertificateOutput;
import com.eventhub.types.response.CertificateVerificationResponse;
import com.eventhub.utilities.result.DataResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateManagerTest {

    @Mock private CertificateDao certificateDao;
    @Mock private RegistrationDao registrationDao;
    @Mock private CertificatePdfService certificatePdfService;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuditService auditService;

    @InjectMocks
    private CertificateManager certificateManager;

    @Test
    void issue_whenEventNotCompleted_isRejected() {
        UUID registrationId = UUID.randomUUID();
        Event event = Event.builder().id(UUID.randomUUID()).status(EventStatus.PUBLISHED).certificateEnabled(true).build();
        Registration registration = Registration.builder().id(registrationId).event(event)
                .status(RegistrationStatus.CHECKED_IN).build();
        when(registrationDao.findById(registrationId)).thenReturn(Optional.of(registration));

        DataResult<CertificateOutput> result = certificateManager.issue(registrationId);

        assertFalse(result.isSuccess());
        verify(certificateDao, never()).save(any());
    }

    @Test
    void issue_whenCertificateNotEnabled_isRejected() {
        UUID registrationId = UUID.randomUUID();
        Event event = Event.builder().id(UUID.randomUUID()).status(EventStatus.COMPLETED).certificateEnabled(false).build();
        Registration registration = Registration.builder().id(registrationId).event(event)
                .status(RegistrationStatus.CHECKED_IN).build();
        when(registrationDao.findById(registrationId)).thenReturn(Optional.of(registration));

        DataResult<CertificateOutput> result = certificateManager.issue(registrationId);

        assertFalse(result.isSuccess());
        verify(certificateDao, never()).save(any());
    }

    @Test
    void issue_whenNotCheckedIn_isRejected() {
        UUID registrationId = UUID.randomUUID();
        Event event = Event.builder().id(UUID.randomUUID()).status(EventStatus.COMPLETED).certificateEnabled(true).build();
        Registration registration = Registration.builder().id(registrationId).event(event)
                .status(RegistrationStatus.REGISTERED).build();
        when(registrationDao.findById(registrationId)).thenReturn(Optional.of(registration));

        DataResult<CertificateOutput> result = certificateManager.issue(registrationId);

        assertFalse(result.isSuccess());
        verify(certificateDao, never()).save(any());
    }

    @Test
    void issue_whenEligible_createsCertificate() throws Exception {
        UUID registrationId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).firstName("Ada").lastName("Lovelace").build();
        Event event = Event.builder().id(UUID.randomUUID()).title("Test Event")
                .status(EventStatus.COMPLETED).certificateEnabled(true).build();
        Registration registration = Registration.builder().id(registrationId).event(event).user(user)
                .status(RegistrationStatus.CHECKED_IN).build();
        when(registrationDao.findById(registrationId)).thenReturn(Optional.of(registration));
        when(certificateDao.findByRegistrationId(registrationId)).thenReturn(Optional.empty());
        when(certificatePdfService.renderAndStore(any(), any(), any(), any(), any(), any())).thenReturn("certificates/x.pdf");

        DataResult<CertificateOutput> result = certificateManager.issue(registrationId);

        assertTrue(result.isSuccess());
        verify(certificateDao).save(argThat(c -> c.getCode() != null && !c.getCode().isBlank()));
    }

    @Test
    void verifyByCode_whenFound_returnsValid() {
        Event event = Event.builder().id(UUID.randomUUID()).title("Test Event").build();
        User user = User.builder().id(UUID.randomUUID()).firstName("Ada").lastName("Lovelace").build();
        Registration registration = Registration.builder().id(UUID.randomUUID()).event(event).user(user).build();
        Certificate certificate = Certificate.builder().id(UUID.randomUUID()).code("ABC123")
                .registration(registration).revoked(false).build();
        when(certificateDao.findByCode("ABC123")).thenReturn(Optional.of(certificate));

        CertificateVerificationResponse response = certificateManager.verifyByCode("ABC123");

        assertTrue(response.isValid());
        assertEquals("Test Event", response.getEventTitle());
    }

    @Test
    void verifyByCode_whenNotFound_returnsInvalid() {
        when(certificateDao.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        CertificateVerificationResponse response = certificateManager.verifyByCode("UNKNOWN");

        assertFalse(response.isValid());
    }
}
