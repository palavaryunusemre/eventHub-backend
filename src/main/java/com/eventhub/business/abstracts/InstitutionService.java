package com.eventhub.business.abstracts;

import com.eventhub.types.output.InstitutionOutput;
import com.eventhub.types.request.InstitutionRequest;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;

import java.util.List;
import java.util.UUID;

public interface InstitutionService {
    DataResult<List<InstitutionOutput>> getAll();
    Result<String> create(InstitutionRequest request);
    Result<String> update(UUID id, InstitutionRequest request);
    Result<String> delete(UUID id);
}
