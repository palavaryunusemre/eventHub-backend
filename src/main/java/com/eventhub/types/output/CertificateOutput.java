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
public class CertificateOutput {
    private UUID id;
    private UUID registrationId;
    private UUID eventId;
    private String eventTitle;
    private String code;
    private String pdfPath;
    private Date issuedAt;
    private Boolean revoked;
}
