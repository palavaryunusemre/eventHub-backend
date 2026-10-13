package com.eventhub.api.controller;

import com.eventhub.business.abstracts.CategoryService;
import com.eventhub.types.output.CategoryOutput;
import com.eventhub.types.request.CategoryRequest;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.eventhub.config.RestApis.CATEGORY;

@RestController
@RequestMapping(CATEGORY)
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<DataResult<List<CategoryOutput>>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @PostMapping
    public ResponseEntity<Result<String>> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<String>> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> delete(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.delete(id));
    }
}
