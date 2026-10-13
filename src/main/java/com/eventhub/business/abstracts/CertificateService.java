package com.eventhub.business.abstracts;

import com.eventhub.types.output.CertificateOutput;
import com.eventhub.types.response.CertificateVerificationResponse;
import com.eventhub.utilities.result.DataResult;

import java.util.List;
import java.util.UUID;

public interface CertificateService {
    DataResult<CertificateOutput> issue(UUID registrationId);
    DataResult<List<CertificateOutput>> getMine();
    CertificateVerificationResponse verifyByCode(String code);
    /** PDF dosyasının ham baytlarını döner; sertifika ya da dosya bulunamazsa null. */
    byte[] getPdfBytes(String code);
}
