package com.heddy.adapter.in.web.summary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.summary.model.MySummary;
import io.swagger.v3.oas.annotations.media.Schema;

public record MySummaryResponse(
        @Schema(description = "내 시술 기록 수")
        @JsonProperty("treatment_record_count") long treatmentRecordCount,

        @Schema(description = "분석이 성공한 시술 기록 수")
        @JsonProperty("analyzed_record_count") long analyzedRecordCount,

        @Schema(description = "저장한 후보 스타일 수")
        @JsonProperty("saved_style_count") long savedStyleCount,

        @Schema(description = "살아있는 공유에 포함된 시술 기록 수. 공유 링크 수가 아니다")
        @JsonProperty("shared_record_count") long sharedRecordCount
) {

    public static MySummaryResponse from(MySummary summary) {
        return new MySummaryResponse(
                summary.treatmentRecordCount(), summary.analyzedRecordCount(),
                summary.savedStyleCount(), summary.sharedRecordCount());
    }
}
