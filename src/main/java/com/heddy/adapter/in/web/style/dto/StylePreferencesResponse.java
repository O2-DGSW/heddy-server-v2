package com.heddy.adapter.in.web.style.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.style.port.in.StylePreferencesResult;

import java.util.List;
import java.util.UUID;

public record StylePreferencesResponse(
        @JsonProperty("preferred_tag_ids") List<UUID> preferredTagIds,
        @JsonProperty("excluded_tag_ids") List<UUID> excludedTagIds
) {
    public static StylePreferencesResponse from(StylePreferencesResult result) {
        return new StylePreferencesResponse(
                result.preferredTagIds(), result.excludedTagIds());
    }
}
