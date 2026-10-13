package com.eventhub.business.abstracts;

import com.eventhub.types.output.CheckInOutput;
import com.eventhub.utilities.result.DataResult;

public interface CheckInService {
    DataResult<CheckInOutput> scan(String qrToken);
}
