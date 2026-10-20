package com.heddy.adapter.in.web.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record GenerateRecommendationRequest(
        @Schema(description = "true이면 새 실행을 만들고 동일 입력의 최근 20회 추천에서 덜 노출된 적합한 스타일을 우선 선택합니다. "
                + "후보가 부족하면 이전 스타일이 포함될 수 있습니다.")
        @JsonProperty("force_refresh") Boolean forceRefresh
) {
    public boolean shouldForceRefresh() {
        return Boolean.TRUE.equals(forceRefresh);
    }
}
