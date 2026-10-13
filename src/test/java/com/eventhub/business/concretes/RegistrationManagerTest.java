package com.eventhub.business.concretes;

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
import com.eventhub.utilities.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationManagerTest {

    @Mock private RegistrationDao registrationDao;
    @Mock private EventDao eventDao;
    @Mock private UserDao userDao;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuditService auditService;

    @InjectMocks
    private RegistrationManager registrationManager;

    private UUID userId;
    private User user;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("attendee@test.com").build();
        eventId = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
    }

    @Test
    void register_whenEventNotPublished_isRejected() {
        when(userDao.findById(userId)).thenReturn(Optional.of(user));
        Event event = Event.builder().id(eventId).status(EventStatus.DRAFT).build();
        when(eventDao.findById(eventId)).thenReturn(Optional.of(event));

        Result<String> result = registrationManager.register(eventId);

        assertFalse(result.isSuccess());
        assertEquals(409, result.getCode());
        verify(registrationDao, never()).save(any());
    }

    @Test
    void register_whenCapacityFull_isRejected() {
        when(userDao.findById(userId)).thenReturn(Optional.of(user));
        Event event = Event.builder().id(eventId).status(EventStatus.PUBLISHED).visibility(EventVisibility.PUBLIC)
                .capacity(1).build();
        when(eventDao.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationDao.findAllByEventIdAndUserIdAndStatus(eq(eventId), eq(userId), any())).thenReturn(List.of());
        when(registrationDao.findAllByEventIdAndStatusNot(eventId, RegistrationStatus.CANCELLED))
                .thenReturn(List.of(new Registration()));

        Result<String> result = registrationManager.register(eventId);

        assertFalse(result.isSuccess());
        assertEquals(409, result.getCode());
        verify(registrationDao, never()).save(any());
    }

    @Test
    void register_whenDuplicateActiveRegistration_isRejected() {
        when(userDao.findById(userId)).thenReturn(Optional.of(user));
        Event event = Event.builder().id(eventId).status(EventStatus.PUBLISHED).visibility(EventVisibility.PUBLIC).build();
        when(eventDao.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationDao.findAllByEventIdAndUserIdAndStatus(eventId, userId, RegistrationStatus.REGISTERED))
                .thenReturn(List.of(new Registration()));

        Result<String> result = registrationManager.register(eventId);

        assertFalse(result.isSuccess());
        assertEquals(409, result.getCode());
        verify(registrationDao, never()).save(any());
    }

    @Test
    void register_whenInstitutionOnlyAndUserNotMember_isRejected() {
        when(userDao.findById(userId)).thenReturn(Optional.of(user));
        Institution institution = Institution.builder().id(UUID.randomUUID()).name("Uni A").build();
        Event event = Event.builder().id(eventId).status(EventStatus.PUBLISHED)
                .visibility(EventVisibility.INSTITUTION_ONLY)
                .allowedInstitutions(Set.of(institution)).build();
        when(eventDao.findById(eventId)).thenReturn(Optional.of(event));

        Result<String> result = registrationManager.register(eventId);

        assertFalse(result.isSuccess());
        assertEquals(403, result.getCode());
        verify(registrationDao, never()).save(any());
    }

    @Test
    void register_success_generatesQrToken() {
        when(userDao.findById(userId)).thenReturn(Optional.of(user));
        Event event = Event.builder().id(eventId).status(EventStatus.PUBLISHED).visibility(EventVisibility.PUBLIC).build();
        when(eventDao.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationDao.findAllByEventIdAndUserIdAndStatus(eq(eventId), eq(userId), any())).thenReturn(List.of());

        Result<String> result = registrationManager.register(eventId);

        assertTrue(result.isSuccess());
        verify(registrationDao).save(argThat(reg -> reg.getQrToken() != null && !reg.getQrToken().isBlank()
                && reg.getStatus() == RegistrationStatus.REGISTERED));
    }

    @Test
    void cancel_byNonOwner_isForbidden() {
        UUID registrationId = UUID.randomUUID();
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        Registration registration = Registration.builder().id(registrationId).user(otherUser)
                .status(RegistrationStatus.REGISTERED).build();
        when(registrationDao.findById(registrationId)).thenReturn(Optional.of(registration));

        Result<String> result = registrationManager.cancel(registrationId);

        assertFalse(result.isSuccess());
        assertEquals(403, result.getCode());
    }

    @Test
    void cancel_byOwner_succeeds() {
        UUID registrationId = UUID.randomUUID();
        Registration registration = Registration.builder().id(registrationId).user(user)
                .status(RegistrationStatus.REGISTERED).build();
        when(registrationDao.findById(registrationId)).thenReturn(Optional.of(registration));

        Result<String> result = registrationManager.cancel(registrationId);

        assertTrue(result.isSuccess());
        assertEquals(RegistrationStatus.CANCELLED, registration.getStatus());
    }
}
