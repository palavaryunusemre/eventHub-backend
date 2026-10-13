package com.eventhub.api.controller;

import com.eventhub.business.abstracts.EventService;
import com.eventhub.types.output.EventOutput;
import com.eventhub.types.request.EventReviewRequest;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.eventhub.config.RestApis.ADMIN_EVENT;

@RestController
@RequestMapping(ADMIN_EVENT)
@RequiredArgsConstructor
public class AdminEventController {
    private final EventService eventService;

    @GetMapping("/pending")
    public ResponseEntity<DataResult<List<EventOutput>>> getPending() {
        return ResponseEntity.ok(eventService.getPendingReview());
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<Result<String>> review(@PathVariable UUID id, @Valid @RequestBody EventReviewRequest request) {
        return ResponseEntity.ok(eventService.review(id, request));
    }
}
