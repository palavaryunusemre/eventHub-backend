package com.eventhub.api.controller;

import com.eventhub.business.abstracts.EventService;
import com.eventhub.types.output.EventOutput;
import com.eventhub.types.output.EventSummaryOutput;
import com.eventhub.types.request.EventCreateRequest;
import com.eventhub.types.request.EventUpdateRequest;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.eventhub.config.RestApis.EVENT;

@RestController
@RequestMapping(EVENT)
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<DataResult<List<EventSummaryOutput>>> discover(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId) {
        return ResponseEntity.ok(eventService.discover(search, categoryId));
    }

    @GetMapping("/mine")
    public ResponseEntity<DataResult<List<EventOutput>>> getMine() {
        return ResponseEntity.ok(eventService.getMine());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResult<EventOutput>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    @PostMapping
    public ResponseEntity<DataResult<UUID>> create(@Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.ok(eventService.create(request));
    }

    @PutMapping
    public ResponseEntity<Result<String>> update(@Valid @RequestBody EventUpdateRequest request) {
        return ResponseEntity.ok(eventService.update(request));
    }

    @PostMapping("/{id}/submit-for-review")
    public ResponseEntity<Result<String>> submitForReview(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.submitForReview(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.cancel(id));
    }
}
