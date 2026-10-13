package com.eventhub.types.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RegistrationOutput {
    private UUID id;
    private UUID eventId;
    private String eventTitle;
    private Date eventStartAt;
    private Date eventEndAt;
    private UUID userId;
    private String userEmail;
    private String status;
    private String qrToken;
    private Date registeredAt;
    private Date checkedInAt;
}
