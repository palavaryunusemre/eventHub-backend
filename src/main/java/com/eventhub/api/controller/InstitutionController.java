package com.eventhub.api.controller;

import com.eventhub.business.abstracts.InstitutionService;
import com.eventhub.types.output.InstitutionOutput;
import com.eventhub.types.request.InstitutionRequest;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.eventhub.config.RestApis.INSTITUTION;

@RestController
@RequestMapping(INSTITUTION)
@RequiredArgsConstructor
public class InstitutionController {
    private final InstitutionService institutionService;

    @GetMapping
    public ResponseEntity<DataResult<List<InstitutionOutput>>> getAll() {
        return ResponseEntity.ok(institutionService.getAll());
    }

    @PostMapping
    public ResponseEntity<Result<String>> create(@Valid @RequestBody InstitutionRequest request) {
        return ResponseEntity.ok(institutionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<String>> update(@PathVariable UUID id, @Valid @RequestBody InstitutionRequest request) {
        return ResponseEntity.ok(institutionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> delete(@PathVariable UUID id) {
        return ResponseEntity.ok(institutionService.delete(id));
    }
}
