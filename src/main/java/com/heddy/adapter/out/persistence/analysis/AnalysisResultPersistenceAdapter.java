package com.heddy.adapter.out.persistence.analysis;

import com.heddy.domain.analysis.model.AnalysisResult;
import com.heddy.domain.analysis.port.out.AnalysisResultRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AnalysisResultPersistenceAdapter implements AnalysisResultRepositoryPort {

    private final AnalysisResultJpaRepository resultRepository;

    @Override
    public AnalysisResult insert(AnalysisResult result) {
        return resultRepository.saveAndFlush(new AnalysisResultEntity(result)).toDomain();
    }

    @Override
    public Optional<AnalysisResult> findByIdAndUserId(UUID analysisId, UUID userId) {
        return resultRepository.findByAnalysisIdAndUserId(analysisId, userId)
                .map(AnalysisResultEntity::toDomain);
    }

    @Override
    public Optional<AnalysisResult> findLatestByRecordId(UUID recordId) {
        return resultRepository.findLatestByRecordId(recordId, Limit.of(1)).stream()
                .findFirst()
                .map(AnalysisResultEntity::toDomain);
    }

    @Override
    public Optional<AnalysisResult> findByJobId(UUID jobId) {
        return resultRepository.findByJobId(jobId).map(AnalysisResultEntity::toDomain);
    }
}
