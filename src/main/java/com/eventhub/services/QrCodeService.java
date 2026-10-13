package com.eventhub.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * ZXing tabanlı QR kod üretim servisi. {@code Registration.qrToken} (check-in QR'ı)
 * ve {@code Certificate.code} (doğrulama QR'ı) için PNG üretir.
 */
@Service
public class QrCodeService {

    private static final int DEFAULT_SIZE = 300;

    /** Verilen metin için PNG formatında QR kod üretir. */
    public byte[] generatePng(String content, int size) throws WriterException, IOException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return outputStream.toByteArray();
        }
    }

    public byte[] generatePng(String content) throws WriterException, IOException {
        return generatePng(content, DEFAULT_SIZE);
    }

    /** Thymeleaf şablonlarında <img src="data:image/png;base64,..."> olarak kullanılabilecek Base64 string üretir. */
    public String generateBase64Png(String content) throws WriterException, IOException {
        return Base64.getEncoder().encodeToString(generatePng(content));
    }
}
