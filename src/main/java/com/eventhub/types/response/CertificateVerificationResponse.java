package com.eventhub.types.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CertificateVerificationResponse {
    private boolean valid;
    private String code;
    private String eventTitle;
    private String attendeeName;
    private Date issuedAt;
    private Date eventStartAt;
    private Date eventEndAt;
    private String message;
}
