package com.heddy.adapter.in.web.style.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.style.model.SavedStyle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "저장한 후보 스타일")
public record SavedStyleResponse(
        @JsonProperty("saved_style_id") UUID savedStyleId,
        @JsonProperty("style_name") String styleName,
        @JsonProperty("image_url") String imageUrl,
        String reason,
        String memo,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static SavedStyleResponse from(SavedStyle savedStyle) {
        return new SavedStyleResponse(
                savedStyle.savedStyleId(), savedStyle.styleName(), savedStyle.imageUrl(),
                savedStyle.reason(), savedStyle.memo(), savedStyle.createdAt(),
                savedStyle.updatedAt());
    }
}
