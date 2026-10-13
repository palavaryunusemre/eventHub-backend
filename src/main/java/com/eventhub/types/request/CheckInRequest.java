package com.eventhub.types.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CheckInRequest {
    @NotBlank(message = "QR token zorunlu")
    private String qrToken;
}
