package com.heddy.adapter.out.persistence.analysis;

import com.heddy.domain.analysis.model.AnalysisJob;
import com.heddy.domain.analysis.model.AnalysisJobStatus;
import com.heddy.domain.analysis.exception.AnalysisError;
import com.heddy.domain.analysis.exception.AnalysisException;
import com.heddy.domain.analysis.port.out.AnalysisJobRepositoryPort;
import com.heddy.domain.analysis.port.out.LatestAnalysisStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AnalysisJobPersistenceAdapter
        implements AnalysisJobRepositoryPort, LatestAnalysisStatusPort {

    private static final List<String> IN_PROGRESS = List.of(
            AnalysisJobStatus.PENDING.name(), AnalysisJobStatus.PROCESSING.name());

    private final AnalysisJobJpaRepository jobRepository;

    @Override
    public AnalysisJob insert(AnalysisJob job) {
        return jobRepository.saveAndFlush(new AnalysisJobEntity(job)).toDomain();
    }

    @Override
    public AnalysisJob update(AnalysisJob job) {
        return jobRepository.findById(job.jobId())
                .map(entity -> {
                    entity.apply(job);
                    return jobRepository.saveAndFlush(entity);
                })
                .map(AnalysisJobEntity::toDomain)
                .orElseThrow(() -> new IllegalStateException("수정할 분석 작업이 존재하지 않습니다."));
    }

    @Override
    public Optional<AnalysisJob> findByIdAndUserId(UUID jobId, UUID userId) {
        return jobRepository.findByJobIdAndUserId(jobId, userId).map(AnalysisJobEntity::toDomain);
    }

    @Override
    public Optional<AnalysisJob> findLatestByRecordId(UUID recordId) {
        return jobRepository.findLatestByRecordId(recordId, Limit.of(1)).stream()
                .findFirst()
                .map(AnalysisJobEntity::toDomain);
    }

    @Override
    public Map<UUID, AnalysisJobStatus> findLatestStatuses(Collection<UUID> recordIds) {
        // 빈 IN 절은 방언에 따라 문법 오류가 되고, 어차피 답이 정해져 있어 질의하지 않는다.
        if (recordIds.isEmpty()) {
            return Map.of();
        }
        return jobRepository.findLatestStatuses(recordIds).stream()
                .collect(Collectors.toMap(
                        AnalysisJobJpaRepository.LatestJobStatusRow::getRecordId,
                        row -> parseStatus(row.getStatus())));
    }

    private static AnalysisJobStatus parseStatus(String status) {
        try {
            return AnalysisJobStatus.valueOf(status);
        } catch (IllegalArgumentException invalidName) {
            throw new AnalysisException(AnalysisError.JOB_STATUS_UNKNOWN);
        }
    }

    @Override
    public Optional<AnalysisJob> findInProgressByPhotoId(UUID photoId) {
        return jobRepository.findByPhotoIdAndStatusIn(photoId, IN_PROGRESS)
                .map(AnalysisJobEntity::toDomain);
    }
}
