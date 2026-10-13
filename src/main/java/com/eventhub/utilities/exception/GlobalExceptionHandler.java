package com.eventhub.utilities.exception;

import com.nimbusds.jose.JOSEException;
import com.eventhub.utilities.result.Result;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.text.ParseException;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<?>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.toList());
        return ResponseEntity.ok(new Result(false,5000,errors));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        String requestUri = attributes.getRequest().getRequestURI();
        String requestMethod = attributes.getRequest().getMethod();

        logger.error("handleGlobalException: " + ex.getMessage());
        logger.error("Error occurred while processing request: " + requestMethod + " " + requestUri);
        logger.error("handleGlobalException: "+ex.getMessage());
        return new ResponseEntity<>("Bir hata oluştu. Lütfen daha sonra tekrar deneyin.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(JOSEException.class)
    public ResponseEntity<?> handleJOSEException(JOSEException ex) {
        logger.error("handleJOSEException: "+ex.getMessage());
        return new ResponseEntity<>("Kimlik doğrulama hatası: Yetkilendirme başarısız.", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> handleIOException(IOException ex) {
        logger.error("handleIOException: "+ex.getMessage());
        return new ResponseEntity<>("Giriş/Çıkış hatası: Sistem bir sorun yaşadı.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ParseException.class)
    public ResponseEntity<?> handleParseException(ParseException ex) {
        logger.error("handleParseException: "+ex.getMessage());
        return new ResponseEntity<>("Veri işleme hatası: Geçersiz veri formatı.", HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(ParserConfigurationException.class)
    public ResponseEntity<?> handleParserConfigurationException(ParserConfigurationException ex) {
        logger.error("handleParserConfigurationException: " + ex.getMessage());
        return new ResponseEntity<>( "XML yapılandırma hatası", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(SAXException.class)
    public ResponseEntity<?> handleSAXException(SAXException ex) {
        logger.error("handleSAXException: " + ex.getMessage());
        return new ResponseEntity<>("XML ayrıştırma hatası", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(XPathExpressionException.class)
    public ResponseEntity<?> handleXPathExpressionException(XPathExpressionException ex) {
        logger.error("handleXPathExpressionException: " + ex.getMessage());
        return new ResponseEntity<>("XPath ifade hatası", HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        logger.error("handleMethodArgumentTypeMismatchException: "+ ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new Result<>(false, 5005, "Lütfen gerekli alanları doldurunuz!"));
    }
}