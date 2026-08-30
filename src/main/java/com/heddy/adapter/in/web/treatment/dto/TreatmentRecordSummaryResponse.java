package com.heddy.adapter.in.web.treatment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.port.in.ListTreatmentRecordsUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** 타임라인 목록에 필요한 시술기록 요약 응답. */
public record TreatmentRecordSummaryResponse(
        @JsonProperty("record_id") UUID recordId,
        @JsonProperty("performed_at") Instant performedAt,
        @JsonProperty("salon_name") String salonName,
        @JsonProperty("designer_name") String designerName,
        @JsonProperty("service_types") Set<ServiceType> serviceTypes,
        Integer satisfaction,
        @Schema(description = "대표 사진의 짧은 만료 Presigned GET URL. 사진이 없으면 null")
        @JsonProperty("thumbnail_url") String thumbnailUrl,
        @Schema(description = "최신 분석 상태. PENDING PROCESSING SUCCEEDED FAILED UNAVAILABLE "
                + "STALE 중 하나이며, 분석을 요청한 적 없는 기록은 null. \"분석 완료\" 는 "
                + "SUCCEEDED 만 해당한다 — 사진이 바뀌면 STALE 로 전이된다")
        @JsonProperty("analysis_status") String analysisStatus,

        @Schema(description = "지금 공유 중인 기록인지. 철회되지 않고 만료도 되지 않은 공유 링크에 "
                + "담겨 있으면 true")
        @JsonProperty("is_shared") boolean shared
) {
    public static TreatmentRecordSummaryResponse from(ListTreatmentRecordsUseCase.Item item) {
        var record = item.record();
        return new TreatmentRecordSummaryResponse(
                record.recordId(), record.performedAt(), record.salonName(), record.designerName(),
                record.serviceTypes(), record.satisfaction(),
                item.thumbnailUrl() == null ? null : item.thumbnailUrl().toString(),
                item.analysisStatus(), item.shared());
    }
}
