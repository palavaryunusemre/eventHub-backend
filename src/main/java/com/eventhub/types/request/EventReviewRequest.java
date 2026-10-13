package com.eventhub.types.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class EventReviewRequest {
    @NotNull(message = "Onay/red bilgisi zorunlu")
    private Boolean approved;
    private String rejectionReason;
}
