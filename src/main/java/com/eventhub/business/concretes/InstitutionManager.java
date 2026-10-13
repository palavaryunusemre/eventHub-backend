package com.eventhub.business.concretes;

import com.eventhub.business.abstracts.InstitutionService;
import com.eventhub.dataAccess.InstitutionDao;
import com.eventhub.entities.Institution;
import com.eventhub.types.enums.InstitutionType;
import com.eventhub.types.output.InstitutionOutput;
import com.eventhub.types.request.InstitutionRequest;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstitutionManager implements InstitutionService {
    private final InstitutionDao institutionDao;

    @Override
    public DataResult<List<InstitutionOutput>> getAll() {
        List<InstitutionOutput> outputs = institutionDao.findAllByDeleteFlag(false).stream()
                .map(this::toOutput).collect(Collectors.toList());
        return new DataResult<>(outputs, true, 200);
    }

    @Override
    public Result<String> create(InstitutionRequest request) {
        if (Arrays.stream(InstitutionType.values()).noneMatch(t -> t.name().equalsIgnoreCase(request.getType()))) {
            return new Result<>(false, 400, "Geçersiz kurum türü.");
        }
        Institution institution = Institution.builder()
                .name(request.getName().trim())
                .emailDomain(request.getEmailDomain() != null ? request.getEmailDomain().trim() : null)
                .type(InstitutionType.valueOf(request.getType().trim().toUpperCase(Locale.ROOT)))
                .verified(false)
                .deleteFlag(false)
                .build();
        institutionDao.save(institution);
        return new Result<>(true, 200, "Kurum oluşturuldu.");
    }

    @Transactional
    @Override
    public Result<String> update(UUID id, InstitutionRequest request) {
        Optional<Institution> found = institutionDao.findById(id);
        if (found.isEmpty() || Boolean.TRUE.equals(found.get().getDeleteFlag())) {
            return new Result<>(false, 404, "Geçersiz kurum.");
        }
        if (Arrays.stream(InstitutionType.values()).noneMatch(t -> t.name().equalsIgnoreCase(request.getType()))) {
            return new Result<>(false, 400, "Geçersiz kurum türü.");
        }
        Institution institution = found.get();
        institution.setName(request.getName().trim());
        institution.setEmailDomain(request.getEmailDomain() != null ? request.getEmailDomain().trim() : null);
        institution.setType(InstitutionType.valueOf(request.getType().trim().toUpperCase(Locale.ROOT)));
        institutionDao.saveAndFlush(institution);
        return new Result<>(true, 200, "Kurum güncellendi.");
    }

    @Transactional
    @Override
    public Result<String> delete(UUID id) {
        Optional<Institution> found = institutionDao.findById(id);
        if (found.isEmpty()) return new Result<>(false, 404, "Geçersiz kurum.");
        found.get().setDeleteFlag(true);
        institutionDao.saveAndFlush(found.get());
        return new Result<>(true, 200, "Kurum silindi.");
    }

    private InstitutionOutput toOutput(Institution institution) {
        return InstitutionOutput.builder()
                .id(institution.getId())
                .name(institution.getName())
                .emailDomain(institution.getEmailDomain())
                .type(institution.getType() != null ? institution.getType().name() : null)
                .verified(institution.getVerified())
                .createdAt(institution.getCreatedAt())
                .build();
    }
}
