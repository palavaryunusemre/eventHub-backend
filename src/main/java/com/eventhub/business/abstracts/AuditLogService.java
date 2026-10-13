package com.eventhub.business.abstracts;

import com.eventhub.types.output.AuditLogOutput;
import com.eventhub.utilities.result.DataResult;

import java.util.Date;
import java.util.List;

public interface AuditLogService {

    DataResult<List<AuditLogOutput>> getLogs(
            int page, int size,
            Date from, Date to,
            String action, String targetType, String email);
}
