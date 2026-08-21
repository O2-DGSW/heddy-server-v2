package com.heddy.adapter.in.web.style.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.style.port.in.SaveStylePreferencesCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record StylePreferencesRequest(
        @NotNull
        @Schema(description = "선호 스타일 태그 ID 목록. 중복 제거 후 최대 10개")
        @JsonProperty("preferred_tag_ids") List<@NotNull UUID> preferredTagIds,
        @NotNull
        @Schema(description = "추천에서 제외할 스타일 태그 ID 목록. 중복 제거 후 최대 10개")
        @JsonProperty("excluded_tag_ids") List<@NotNull UUID> excludedTagIds
) {
    public SaveStylePreferencesCommand toCommand(UUID userId) {
        return new SaveStylePreferencesCommand(userId, preferredTagIds, excludedTagIds);
    }
}
