package com.eventhub.api.controller;

import com.eventhub.business.abstracts.CertificateService;
import com.eventhub.types.output.CertificateOutput;
import com.eventhub.types.response.CertificateVerificationResponse;
import com.eventhub.utilities.result.DataResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.eventhub.config.RestApis.CERTIFICATE;
import static com.eventhub.config.RestApis.VERIFY;

@RestController
@RequiredArgsConstructor
public class CertificateController {
    private final CertificateService certificateService;

    @PostMapping(CERTIFICATE + "/{registrationId}/issue")
    public ResponseEntity<DataResult<CertificateOutput>> issue(@PathVariable UUID registrationId) {
        return ResponseEntity.ok(certificateService.issue(registrationId));
    }

    @GetMapping(CERTIFICATE + "/mine")
    public ResponseEntity<DataResult<List<CertificateOutput>>> getMine() {
        return ResponseEntity.ok(certificateService.getMine());
    }

    /** Herkese açık sertifika doğrulama uç noktası (örn. QR kod okutulduğunda açılır). */
    @GetMapping(VERIFY + "/{code}")
    public ResponseEntity<CertificateVerificationResponse> verify(@PathVariable String code) {
        return ResponseEntity.ok(certificateService.verifyByCode(code));
    }

    @GetMapping(CERTIFICATE + "/{code}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String code) {
        byte[] pdf = certificateService.getPdfBytes(code);
        if (pdf == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + code + ".pdf\"")
                .body(pdf);
    }
}
