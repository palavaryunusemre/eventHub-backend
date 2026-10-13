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
public class CheckInOutput {
    private UUID id;
    private UUID registrationId;
    private UUID eventId;
    private String eventTitle;
    private UUID userId;
    private String userEmail;
    private String userName;
    private Date scannedAt;
    private UUID scannedById;
}
