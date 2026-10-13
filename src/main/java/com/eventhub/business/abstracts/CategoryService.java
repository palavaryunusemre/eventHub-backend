package com.eventhub.business.abstracts;

import com.eventhub.types.output.CategoryOutput;
import com.eventhub.types.request.CategoryRequest;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    DataResult<List<CategoryOutput>> getAll();
    Result<String> create(CategoryRequest request);
    Result<String> update(UUID id, CategoryRequest request);
    Result<String> delete(UUID id);
}
