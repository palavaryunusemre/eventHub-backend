package com.eventhub.services;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;

/**
 * Thymeleaf HTML şablonu + OpenHTMLtoPDF ile sertifika PDF render eder.
 * Şablon: {@code src/main/resources/templates/certificate.html}.
 * Spring Boot'un web view resolver'ına bağlı olmadan, kendi bağımsız (standalone)
 * TemplateEngine'ini kurar — PDF render'ı bir HTTP view değildir.
 */
@Service
public class CertificatePdfService {

    private static final String STORAGE_DIR = "certificates";

    private final TemplateEngine templateEngine;
    private final QrCodeService qrCodeService;

    public CertificatePdfService(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public String renderAndStore(String certificateCode, String attendeeName, String eventTitle,
                                  Date eventStartAt, Date issuedAt, String verificationUrl) throws IOException {
        byte[] pdf = render(certificateCode, attendeeName, eventTitle, eventStartAt, issuedAt, verificationUrl);

        Path dir = Path.of(STORAGE_DIR);
        Files.createDirectories(dir);
        Path file = dir.resolve(certificateCode + "-" + UUID.randomUUID().toString().substring(0, 8) + ".pdf");
        Files.write(file, pdf);
        return file.toString();
    }

    public byte[] render(String certificateCode, String attendeeName, String eventTitle,
                          Date eventStartAt, Date issuedAt, String verificationUrl) throws IOException {
        Context ctx = new Context();
        ctx.setVariable("certificateCode", certificateCode);
        ctx.setVariable("attendeeName", attendeeName);
        ctx.setVariable("eventTitle", eventTitle);
        ctx.setVariable("eventStartAt", eventStartAt);
        ctx.setVariable("issuedAt", issuedAt);
        ctx.setVariable("verificationUrl", verificationUrl);
        try {
            ctx.setVariable("qrCodeBase64", qrCodeService.generateBase64Png(verificationUrl));
        } catch (Exception e) {
            ctx.setVariable("qrCodeBase64", "");
        }

        String html = templateEngine.process("certificate", ctx);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        }
    }
}
