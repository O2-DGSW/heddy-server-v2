package com.heddy.adapter.in.web.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.analysis.port.in.GetAnalysisJobUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "비동기 분석 작업 상태")
public record AnalysisJobResponse(
        @JsonProperty("job_id") UUID jobId,
        String status,
        int progress,
        @JsonProperty("attempt_count") int attemptCount,
        @JsonProperty("analysis_id") UUID analysisId,
        Failure failure,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
    public record Failure(String code, String message) {
    }

    public static AnalysisJobResponse from(GetAnalysisJobUseCase.Result result) {
        var job = result.job();
        Failure failure = job.failureCode() == null
                ? null : new Failure(job.failureCode(), job.failureMessage());
        Instant updatedAt = job.finishedAt() != null ? job.finishedAt()
                : job.startedAt() != null ? job.startedAt() : job.createdAt();
        return new AnalysisJobResponse(job.jobId(), job.status().name(), job.progress(),
                job.attemptCount(), result.analysisId(), failure, job.createdAt(), updatedAt);
    }
}
