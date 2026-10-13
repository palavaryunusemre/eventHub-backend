package com.eventhub.business.abstracts;

import com.eventhub.types.output.RegistrationOutput;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;

import java.util.List;
import java.util.UUID;

public interface RegistrationService {
    Result<String> register(UUID eventId);
    Result<String> cancel(UUID registrationId);
    DataResult<List<RegistrationOutput>> getMine();
}
