package com.heddy.adapter.in.web.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.analysis.model.AnalysisJob;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "접수된 비동기 분석 작업")
public record AnalysisJobAcceptedResponse(
        @JsonProperty("job_id") UUID jobId,
        @JsonProperty("record_id") UUID recordId,
        @JsonProperty("photo_id") UUID photoId,
        String status,
        @JsonProperty("created_at") Instant createdAt
) {
    public static AnalysisJobAcceptedResponse from(AnalysisJob job) {
        return new AnalysisJobAcceptedResponse(job.jobId(), job.recordId(), job.photoId(),
                job.status().name(), job.createdAt());
    }
}
