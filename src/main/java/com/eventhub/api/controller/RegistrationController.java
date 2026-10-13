package com.eventhub.api.controller;

import com.eventhub.business.abstracts.RegistrationService;
import com.eventhub.types.output.RegistrationOutput;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.eventhub.config.RestApis.EVENT;
import static com.eventhub.config.RestApis.REGISTRATION;

@RestController
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;

    @PostMapping(EVENT + "/{id}/register")
    public ResponseEntity<Result<String>> register(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.register(id));
    }

    @DeleteMapping(REGISTRATION + "/{id}")
    public ResponseEntity<Result<String>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.cancel(id));
    }

    @GetMapping(REGISTRATION + "/mine")
    public ResponseEntity<DataResult<List<RegistrationOutput>>> getMine() {
        return ResponseEntity.ok(registrationService.getMine());
    }
}
