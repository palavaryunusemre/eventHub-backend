package com.eventhub.api.controller;

import com.eventhub.business.abstracts.AuditLogService;
import com.eventhub.types.output.AuditLogOutput;
import com.eventhub.utilities.result.DataResult;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

import static com.eventhub.config.RestApis.AUDIT;

@RestController
@RequestMapping(AUDIT)
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/logs")
    public ResponseEntity<DataResult<List<AuditLogOutput>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date to,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(auditLogService.getLogs(page, size, from, to, action, targetType, email));
    }
}
