package com.eventhub.business.concretes;

import com.eventhub.dataAccess.CategoryDao;
import com.eventhub.dataAccess.EventDao;
import com.eventhub.dataAccess.InstitutionDao;
import com.eventhub.dataAccess.RegistrationDao;
import com.eventhub.dataAccess.UserDao;
import com.eventhub.entities.Event;
import com.eventhub.entities.User;
import com.eventhub.services.AuditService;
import com.eventhub.services.CurrentUserService;
import com.eventhub.types.enums.EventFormat;
import com.eventhub.types.enums.EventStatus;
import com.eventhub.types.enums.EventVisibility;
import com.eventhub.types.enums.Role;
import com.eventhub.types.request.EventCreateRequest;
import com.eventhub.types.request.EventReviewRequest;
import com.eventhub.types.request.EventUpdateRequest;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventManagerTest {

    @Mock private EventDao eventDao;
    @Mock private CategoryDao categoryDao;
    @Mock private InstitutionDao institutionDao;
    @Mock private UserDao userDao;
    @Mock private RegistrationDao registrationDao;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuditService auditService;

    @InjectMocks
    private EventManager eventManager;

    private UUID ownerId;
    private User owner;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        owner = User.builder().id(ownerId).email("owner@test.com").firstName("Ada").lastName("Lovelace").role(Role.USER).build();
    }

    @Test
    void create_setsStatusToDraft() {
        when(currentUserService.getCurrentUserId()).thenReturn(ownerId);
        when(userDao.findById(ownerId)).thenReturn(Optional.of(owner));

        EventCreateRequest request = EventCreateRequest.builder()
                .title("Spring Boot Semineri")
                .visibility(EventVisibility.PUBLIC.name())
                .format(EventFormat.ONLINE.name())
                .startAt(new Date(System.currentTimeMillis() + 100000))
                .endAt(new Date(System.currentTimeMillis() + 200000))
                .build();

        DataResult<UUID> result = eventManager.create(request);

        assertTrue(result.isSuccess());
        verify(eventDao).save(argThat(event -> event.getStatus() == EventStatus.DRAFT
                && event.getOrganizer().getId().equals(ownerId)));
    }

    @Test
    void review_byNonAdmin_isRejected() {
        when(currentUserService.isAdmin()).thenReturn(false);

        Result<String> result = eventManager.review(UUID.randomUUID(),
                EventReviewRequest.builder().approved(true).build());

        assertFalse(result.isSuccess());
        assertEquals(403, result.getCode());
        verify(eventDao, never()).saveAndFlush(any());
    }

    @Test
    void review_approvesPendingEvent() {
        UUID eventId = UUID.randomUUID();
        Event event = Event.builder().id(eventId).title("Test").organizer(owner).status(EventStatus.PENDING_REVIEW).build();

        when(currentUserService.isAdmin()).thenReturn(true);
        when(eventDao.findById(eventId)).thenReturn(Optional.of(event));
        when(currentUserService.getCurrentUserId()).thenReturn(ownerId);
        when(userDao.findById(ownerId)).thenReturn(Optional.of(owner));

        Result<String> result = eventManager.review(eventId, EventReviewRequest.builder().approved(true).build());

        assertTrue(result.isSuccess());
        assertEquals(EventStatus.PUBLISHED, event.getStatus());
        verify(eventDao).saveAndFlush(event);
    }

    @Test
    void review_rejectsPendingEventWithReason() {
        UUID eventId = UUID.randomUUID();
        Event event = Event.builder().id(eventId).title("Test").organizer(owner).status(EventStatus.PENDING_REVIEW).build();

        when(currentUserService.isAdmin()).thenReturn(true);
        when(eventDao.findById(eventId)).thenReturn(Optional.of(event));
        when(currentUserService.getCurrentUserId()).thenReturn(ownerId);
        when(userDao.findById(ownerId)).thenReturn(Optional.of(owner));

        Result<String> result = eventManager.review(eventId,
                EventReviewRequest.builder().approved(false).rejectionReason("Eksik bilgi").build());

        assertTrue(result.isSuccess());
        assertEquals(EventStatus.REJECTED, event.getStatus());
        assertEquals("Eksik bilgi", event.getRejectionReason());
    }

    @Test
    void update_byNonOwnerNonAdmin_isForbidden() {
        UUID eventId = UUID.randomUUID();
        Event event = Event.builder().id(eventId).title("Test").organizer(owner).status(EventStatus.DRAFT).build();
        UUID otherUserId = UUID.randomUUID();

        when(eventDao.findById(eventId)).thenReturn(Optional.of(event));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(otherUserId);

        EventUpdateRequest request = EventUpdateRequest.builder()
                .id(eventId)
                .title("Yeni başlık")
                .visibility(EventVisibility.PUBLIC.name())
                .format(EventFormat.ONLINE.name())
                .build();

        Result<String> result = eventManager.update(request);

        assertFalse(result.isSuccess());
        assertEquals(403, result.getCode());
    }

    @Test
    void submitForReview_fromDraft_movesToPendingReview() {
        UUID eventId = UUID.randomUUID();
        Event event = Event.builder().id(eventId).title("Test").organizer(owner).status(EventStatus.DRAFT).build();

        when(eventDao.findById(eventId)).thenReturn(Optional.of(event));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(ownerId);

        Result<String> result = eventManager.submitForReview(eventId);

        assertTrue(result.isSuccess());
        assertEquals(EventStatus.PENDING_REVIEW, event.getStatus());
    }
}
