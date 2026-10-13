package com.eventhub.business.concretes;

import com.eventhub.business.abstracts.AuditLogService;
import com.eventhub.dataAccess.AuditLogDao;
import com.eventhub.dataAccess.spefications.AuditLogSpecification;
import com.eventhub.entities.AuditLog;
import com.eventhub.services.JwtService;
import com.eventhub.types.output.AuditLogOutput;
import com.eventhub.types.response.PageInfoResponse;
import com.eventhub.utilities.result.DataResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogManager implements AuditLogService {

    private final AuditLogDao auditLogDao;
    private final JwtService jwtService;

    @Override
    public DataResult<List<AuditLogOutput>> getLogs(
            int page,
            int size,
            Date from,
            Date to,
            String action,
            String targetType,
            String email) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // ADMIN artık global bir rol — tüm kayıtları görür, kuruma göre kapsam sınırlaması yok.
        UUID institutionId = null;

        Specification<AuditLog> spec = AuditLogSpecification.filter(institutionId, from, to, action, targetType, email);
        Page<AuditLog> resultPage = auditLogDao.findAll(spec, pageable);

        List<AuditLogOutput> outputs = resultPage.getContent().stream()
                .map(this::toOutput)
                .collect(Collectors.toList());

        PageInfoResponse pageInfoResponse = PageInfoResponse.builder()
                .currentPage(page)
                .totalPages(resultPage.getTotalPages())
                .totalElements(resultPage.getTotalElements())
                .hasNext(resultPage.hasNext())
                .build();

        return new DataResult<>(outputs, true, 200, pageInfoResponse);
    }

    private AuditLogOutput toOutput(AuditLog l) {
        return AuditLogOutput.builder()
                .id(l.getId())
                .action(l.getAction())
                .targetType(l.getTargetType())
                .targetId(l.getTargetId())
                .details(l.getDetails())
                .performedById(l.getPerformedById())
                .performedByEmail(l.getPerformedByEmail())
                .institutionId(l.getInstitutionId())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
