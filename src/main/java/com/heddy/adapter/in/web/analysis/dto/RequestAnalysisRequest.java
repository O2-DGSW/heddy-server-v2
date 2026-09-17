package com.heddy.adapter.in.web.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** 분석할 사진을 선택한다. 비우면 기록의 첫 번째 AFTER 사진을 사용한다. */
public record RequestAnalysisRequest(
        @Schema(description = "분석할 시술 사진 식별자. 생략하면 대표 AFTER 사진")
        @JsonProperty("photo_id") UUID photoId
) {
}
