package com.eventhub.business.abstracts;

import com.eventhub.types.output.EventOutput;
import com.eventhub.types.output.EventSummaryOutput;
import com.eventhub.types.request.EventCreateRequest;
import com.eventhub.types.request.EventReviewRequest;
import com.eventhub.types.request.EventUpdateRequest;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;

import java.util.List;
import java.util.UUID;

public interface EventService {
    DataResult<List<EventSummaryOutput>> discover(String search, UUID categoryId);
    DataResult<EventOutput> getById(UUID id);
    DataResult<List<EventOutput>> getMine();
    DataResult<List<EventOutput>> getPendingReview();
    DataResult<UUID> create(EventCreateRequest request);
    Result<String> update(EventUpdateRequest request);
    Result<String> submitForReview(UUID id);
    Result<String> review(UUID id, EventReviewRequest request);
    Result<String> cancel(UUID id);
}
