package com.eventhub.business.concretes;

import com.eventhub.dataAccess.CheckInDao;
import com.eventhub.dataAccess.RegistrationDao;
import com.eventhub.dataAccess.UserDao;
import com.eventhub.entities.Event;
import com.eventhub.entities.Registration;
import com.eventhub.entities.User;
import com.eventhub.services.AuditService;
import com.eventhub.services.CurrentUserService;
import com.eventhub.types.enums.RegistrationStatus;
import com.eventhub.types.output.CheckInOutput;
import com.eventhub.utilities.result.DataResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInManagerTest {

    @Mock private CheckInDao checkInDao;
    @Mock private RegistrationDao registrationDao;
    @Mock private UserDao userDao;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuditService auditService;

    @InjectMocks
    private CheckInManager checkInManager;

    @Test
    void scan_withInvalidToken_returnsNotFound() {
        when(registrationDao.findByQrToken("invalid")).thenReturn(Optional.empty());

        DataResult<CheckInOutput> result = checkInManager.scan("invalid");

        assertFalse(result.isSuccess());
        assertEquals(404, result.getCode());
    }

    @Test
    void scan_whenAlreadyCheckedIn_isRejected() {
        Event event = Event.builder().id(UUID.randomUUID())
                .startAt(new Date(System.currentTimeMillis() - 10000))
                .endAt(new Date(System.currentTimeMillis() + 10000)).build();
        Registration registration = Registration.builder().id(UUID.randomUUID()).event(event)
                .qrToken("token-1").status(RegistrationStatus.CHECKED_IN).build();
        when(registrationDao.findByQrToken("token-1")).thenReturn(Optional.of(registration));

        DataResult<CheckInOutput> result = checkInManager.scan("token-1");

        assertFalse(result.isSuccess());
        assertEquals(409, result.getCode());
        verify(checkInDao, never()).save(any());
    }

    @Test
    void scan_whenCancelled_isRejected() {
        Registration registration = Registration.builder().id(UUID.randomUUID())
                .qrToken("token-2").status(RegistrationStatus.CANCELLED).build();
        when(registrationDao.findByQrToken("token-2")).thenReturn(Optional.of(registration));

        DataResult<CheckInOutput> result = checkInManager.scan("token-2");

        assertFalse(result.isSuccess());
        assertEquals(409, result.getCode());
    }

    @Test
    void scan_validToken_marksCheckedInAndCreatesCheckInRecord() {
        UUID scannerId = UUID.randomUUID();
        User scanner = User.builder().id(scannerId).build();
        Event event = Event.builder().id(UUID.randomUUID())
                .startAt(new Date(System.currentTimeMillis() - 10000))
                .endAt(new Date(System.currentTimeMillis() + 10000)).build();
        Registration registration = Registration.builder().id(UUID.randomUUID()).event(event)
                .qrToken("token-3").status(RegistrationStatus.REGISTERED).build();

        when(registrationDao.findByQrToken("token-3")).thenReturn(Optional.of(registration));
        when(currentUserService.getCurrentUserId()).thenReturn(scannerId);
        when(userDao.findById(scannerId)).thenReturn(Optional.of(scanner));

        DataResult<CheckInOutput> result = checkInManager.scan("token-3");

        assertTrue(result.isSuccess());
        assertEquals(RegistrationStatus.CHECKED_IN, registration.getStatus());
        assertNotNull(registration.getCheckedInAt());
        verify(checkInDao).save(any());
    }

    @Test
    void scan_whenEventNotStartedYet_isRejected() {
        Event event = Event.builder().id(UUID.randomUUID())
                .startAt(new Date(System.currentTimeMillis() + 10L * 60 * 60 * 1000))
                .endAt(new Date(System.currentTimeMillis() + 20L * 60 * 60 * 1000)).build();
        Registration registration = Registration.builder().id(UUID.randomUUID()).event(event)
                .qrToken("token-4").status(RegistrationStatus.REGISTERED).build();
        when(registrationDao.findByQrToken("token-4")).thenReturn(Optional.of(registration));

        DataResult<CheckInOutput> result = checkInManager.scan("token-4");

        assertFalse(result.isSuccess());
        assertEquals(409, result.getCode());
        verify(checkInDao, never()).save(any());
    }
}
