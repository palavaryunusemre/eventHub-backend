package com.eventhub.business.concretes;

import com.eventhub.business.abstracts.EventService;
import com.eventhub.dataAccess.CategoryDao;
import com.eventhub.dataAccess.EventDao;
import com.eventhub.dataAccess.InstitutionDao;
import com.eventhub.dataAccess.UserDao;
import com.eventhub.dataAccess.spefications.EventSpecification;
import com.eventhub.entities.Category;
import com.eventhub.entities.Event;
import com.eventhub.entities.Institution;
import com.eventhub.entities.User;
import com.eventhub.services.AuditService;
import com.eventhub.services.CurrentUserService;
import com.eventhub.types.enums.EventFormat;
import com.eventhub.types.enums.EventStatus;
import com.eventhub.types.enums.EventVisibility;
import com.eventhub.types.output.EventOutput;
import com.eventhub.types.output.EventSummaryOutput;
import com.eventhub.types.request.EventCreateRequest;
import com.eventhub.types.request.EventReviewRequest;
import com.eventhub.types.request.EventUpdateRequest;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventManager implements EventService {

    private final EventDao eventDao;
    private final CategoryDao categoryDao;
    private final InstitutionDao institutionDao;
    private final UserDao userDao;
    private final com.eventhub.dataAccess.RegistrationDao registrationDao;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Override
    public DataResult<List<EventSummaryOutput>> discover(String search, UUID categoryId) {
        UUID institutionId = currentUserService.getCurrentUserInstitutionId();
        List<Event> events = eventDao.findAll(EventSpecification.visibleTo(institutionId, search, categoryId));
        return new DataResult<>(events.stream().map(this::toSummary).collect(Collectors.toList()), true, 200);
    }

    @Override
    public DataResult<EventOutput> getById(UUID id) {
        Optional<Event> found = eventDao.findById(id);
        if (found.isEmpty()) return new DataResult<>(false, 404, "Etkinlik bulunamadı.");
        return new DataResult<>(toOutput(found.get()), true, 200);
    }

    @Override
    public DataResult<List<EventOutput>> getMine() {
        UUID userId = currentUserService.getCurrentUserId();
        List<Event> events = eventDao.findAllByOrganizerId(userId);
        return new DataResult<>(events.stream().map(this::toOutput).collect(Collectors.toList()), true, 200);
    }

    @Override
    public DataResult<List<EventOutput>> getPendingReview() {
        List<Event> events = eventDao.findAll(EventSpecification.byStatus(EventStatus.PENDING_REVIEW));
        return new DataResult<>(events.stream().map(this::toOutput).collect(Collectors.toList()), true, 200);
    }

    @Override
    public DataResult<UUID> create(EventCreateRequest request) {
        UUID userId = currentUserService.getCurrentUserId();
        Optional<User> organizer = userDao.findById(userId);
        if (organizer.isEmpty()) return new DataResult<>(false, 401, "Oturum bulunamadı.");

        if (Arrays.stream(EventVisibility.values()).noneMatch(v -> v.name().equalsIgnoreCase(request.getVisibility()))) {
            return new DataResult<>(false, 400, "Geçersiz görünürlük bilgisi.");
        }
        if (Arrays.stream(EventFormat.values()).noneMatch(f -> f.name().equalsIgnoreCase(request.getFormat()))) {
            return new DataResult<>(false, 400, "Geçersiz format bilgisi.");
        }
        if (request.getEndAt() != null && request.getStartAt() != null && request.getEndAt().before(request.getStartAt())) {
            return new DataResult<>(false, 400, "Bitiş tarihi başlangıçtan önce olamaz.");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            Optional<Category> found = categoryDao.findById(request.getCategoryId());
            if (found.isEmpty()) return new DataResult<>(false, 400, "Geçersiz kategori.");
            category = found.get();
        }

        Set<Institution> allowed = resolveInstitutions(request.getAllowedInstitutionIds());

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .title(request.getTitle().trim())
                .slug(generateSlug(request.getTitle()))
                .description(request.getDescription())
                .coverImageUrl(request.getCoverImageUrl())
                .category(category)
                .organizer(organizer.get())
                .visibility(EventVisibility.valueOf(request.getVisibility().trim().toUpperCase(Locale.ROOT)))
                .allowedInstitutions(allowed)
                .format(EventFormat.valueOf(request.getFormat().trim().toUpperCase(Locale.ROOT)))
                .location(request.getLocation())
                .onlineUrl(request.getOnlineUrl())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .capacity(request.getCapacity())
                .status(EventStatus.DRAFT)
                .certificateEnabled(Boolean.TRUE.equals(request.getCertificateEnabled()))
                .build();
        eventDao.save(event);
        auditService.log("EVENT_CREATED", "EVENT", event.getId().toString(), event.getTitle(), null);
        return new DataResult<>(event.getId(), true, 200);
    }

    @Transactional
    @Override
    public Result<String> update(EventUpdateRequest request) {
        Optional<Event> found = eventDao.findById(request.getId());
        if (found.isEmpty()) return new Result<>(false, 404, "Etkinlik bulunamadı.");
        Event event = found.get();

        if (!canModify(event)) return new Result<>(false, 403, "Bu etkinliği düzenleme yetkiniz yok.");
        if (!isEditableStatus(event.getStatus())) {
            return new Result<>(false, 409, "Etkinlik bu durumdayken düzenlenemez.");
        }
        if (Arrays.stream(EventVisibility.values()).noneMatch(v -> v.name().equalsIgnoreCase(request.getVisibility()))) {
            return new Result<>(false, 400, "Geçersiz görünürlük bilgisi.");
        }
        if (Arrays.stream(EventFormat.values()).noneMatch(f -> f.name().equalsIgnoreCase(request.getFormat()))) {
            return new Result<>(false, 400, "Geçersiz format bilgisi.");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            Optional<Category> cat = categoryDao.findById(request.getCategoryId());
            if (cat.isEmpty()) return new Result<>(false, 400, "Geçersiz kategori.");
            category = cat.get();
        }

        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setCoverImageUrl(request.getCoverImageUrl());
        event.setCategory(category);
        event.setVisibility(EventVisibility.valueOf(request.getVisibility().trim().toUpperCase(Locale.ROOT)));
        event.setAllowedInstitutions(resolveInstitutions(request.getAllowedInstitutionIds()));
        event.setFormat(EventFormat.valueOf(request.getFormat().trim().toUpperCase(Locale.ROOT)));
        event.setLocation(request.getLocation());
        event.setOnlineUrl(request.getOnlineUrl());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setCapacity(request.getCapacity());
        event.setCertificateEnabled(Boolean.TRUE.equals(request.getCertificateEnabled()));
        // Reddedilen bir etkinlik düzenlenip yeniden kaydedildiğinde taslağa döner; yeniden incelemeye gönderilmesi gerekir.
        if (event.getStatus() == EventStatus.REJECTED) {
            event.setStatus(EventStatus.DRAFT);
            event.setRejectionReason(null);
        }
        eventDao.saveAndFlush(event);
        auditService.log("EVENT_UPDATED", "EVENT", event.getId().toString(), event.getTitle(), null);
        return new Result<>(true, 200, "Etkinlik güncellendi.");
    }

    @Transactional
    @Override
    public Result<String> submitForReview(UUID id) {
        Optional<Event> found = eventDao.findById(id);
        if (found.isEmpty()) return new Result<>(false, 404, "Etkinlik bulunamadı.");
        Event event = found.get();
        if (!canModify(event)) return new Result<>(false, 403, "Bu etkinliği gönderme yetkiniz yok.");
        if (event.getStatus() != EventStatus.DRAFT) {
            return new Result<>(false, 409, "Sadece taslak etkinlikler incelemeye gönderilebilir.");
        }
        event.setStatus(EventStatus.PENDING_REVIEW);
        eventDao.saveAndFlush(event);
        auditService.log("EVENT_SUBMITTED", "EVENT", event.getId().toString(), event.getTitle(), null);
        return new Result<>(true, 200, "Etkinlik incelemeye gönderildi.");
    }

    @Transactional
    @Override
    public Result<String> review(UUID id, EventReviewRequest request) {
        if (!currentUserService.isAdmin()) return new Result<>(false, 403, "Bu işlem için admin yetkisi gerekir.");
        Optional<Event> found = eventDao.findById(id);
        if (found.isEmpty()) return new Result<>(false, 404, "Etkinlik bulunamadı.");
        Event event = found.get();
        if (event.getStatus() != EventStatus.PENDING_REVIEW) {
            return new Result<>(false, 409, "Sadece inceleme bekleyen etkinlikler onaylanabilir/reddedilebilir.");
        }
        UUID reviewerId = currentUserService.getCurrentUserId();
        User reviewer = userDao.findById(reviewerId).orElse(null);

        if (Boolean.TRUE.equals(request.getApproved())) {
            event.setStatus(EventStatus.PUBLISHED);
            event.setRejectionReason(null);
        } else {
            event.setStatus(EventStatus.REJECTED);
            event.setRejectionReason(request.getRejectionReason());
        }
        event.setReviewedBy(reviewer);
        event.setReviewedAt(new Date());
        eventDao.saveAndFlush(event);
        auditService.log(Boolean.TRUE.equals(request.getApproved()) ? "EVENT_APPROVED" : "EVENT_REJECTED",
                "EVENT", event.getId().toString(), event.getTitle(), null);
        return new Result<>(true, 200, "Etkinlik incelendi.");
    }

    @Transactional
    @Override
    public Result<String> cancel(UUID id) {
        Optional<Event> found = eventDao.findById(id);
        if (found.isEmpty()) return new Result<>(false, 404, "Etkinlik bulunamadı.");
        Event event = found.get();
        if (!canModify(event)) return new Result<>(false, 403, "Bu etkinliği iptal etme yetkiniz yok.");
        event.setStatus(EventStatus.CANCELLED);
        eventDao.saveAndFlush(event);
        auditService.log("EVENT_CANCELLED", "EVENT", event.getId().toString(), event.getTitle(), null);
        return new Result<>(true, 200, "Etkinlik iptal edildi.");
    }

    private boolean canModify(Event event) {
        if (currentUserService.isAdmin()) return true;
        UUID userId = currentUserService.getCurrentUserId();
        return event.getOrganizer() != null && event.getOrganizer().getId().equals(userId);
    }

    private boolean isEditableStatus(EventStatus status) {
        return status == EventStatus.DRAFT || status == EventStatus.PENDING_REVIEW || status == EventStatus.REJECTED;
    }

    private Set<Institution> resolveInstitutions(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        return new HashSet<>(institutionDao.findAllById(ids));
    }

    private String generateSlug(String title) {
        String base = title.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private EventSummaryOutput toSummary(Event e) {
        return EventSummaryOutput.builder()
                .id(e.getId())
                .title(e.getTitle())
                .slug(e.getSlug())
                .coverImageUrl(e.getCoverImageUrl())
                .categoryName(e.getCategory() != null ? e.getCategory().getName() : null)
                .format(e.getFormat() != null ? e.getFormat().name() : null)
                .location(e.getLocation())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .status(e.getStatus() != null ? e.getStatus().name() : null)
                .visibility(e.getVisibility() != null ? e.getVisibility().name() : null)
                .build();
    }

    private EventOutput toOutput(Event e) {
        return EventOutput.builder()
                .id(e.getId())
                .title(e.getTitle())
                .slug(e.getSlug())
                .description(e.getDescription())
                .coverImageUrl(e.getCoverImageUrl())
                .categoryId(e.getCategory() != null ? e.getCategory().getId() : null)
                .categoryName(e.getCategory() != null ? e.getCategory().getName() : null)
                .organizerId(e.getOrganizer() != null ? e.getOrganizer().getId() : null)
                .organizerName(e.getOrganizer() != null ? e.getOrganizer().getFirstName() + " " + e.getOrganizer().getLastName() : null)
                .visibility(e.getVisibility() != null ? e.getVisibility().name() : null)
                .allowedInstitutionIds(e.getAllowedInstitutions() != null
                        ? e.getAllowedInstitutions().stream().map(Institution::getId).collect(Collectors.toSet())
                        : Set.of())
                .format(e.getFormat() != null ? e.getFormat().name() : null)
                .location(e.getLocation())
                .onlineUrl(e.getOnlineUrl())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .capacity(e.getCapacity())
                .registeredCount(registrationDao.findAllByEventIdAndStatusNot(
                        e.getId(), com.eventhub.types.enums.RegistrationStatus.CANCELLED).size())
                .status(e.getStatus() != null ? e.getStatus().name() : null)
                .certificateEnabled(e.getCertificateEnabled())
                .rejectionReason(e.getRejectionReason())
                .reviewedById(e.getReviewedBy() != null ? e.getReviewedBy().getId() : null)
                .reviewedAt(e.getReviewedAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
