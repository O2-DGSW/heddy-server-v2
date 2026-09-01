package com.heddy.adapter.in.web.style.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.style.port.in.StylePreferencesResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "스타일 선호 태그 응답. 한 번도 저장하지 않았으면 두 목록 모두 비어 있다")
public record StylePreferencesResponse(
        @Schema(description = "선호하는 스타일 태그 식별자 목록. 최대 10개")
        @JsonProperty("preferred_tag_ids") List<UUID> preferredTagIds,

        @Schema(description = "제외하고 싶은 스타일 태그 식별자 목록. 최대 10개이며 선호 "
                + "목록과 같은 태그를 넣으면 저장 시 422 STYLE_PREFERENCE_CONFLICT 다")
        @JsonProperty("excluded_tag_ids") List<UUID> excludedTagIds
) {
    public static StylePreferencesResponse from(StylePreferencesResult result) {
        return new StylePreferencesResponse(
                result.preferredTagIds(), result.excludedTagIds());
    }
}
