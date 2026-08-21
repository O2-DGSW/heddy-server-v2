package com.heddy.adapter.in.web.style.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.style.port.in.SaveStylePreferencesCommand;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record StylePreferencesRequest(
        @NotNull
        @JsonProperty("preferred_tag_ids") List<@NotNull UUID> preferredTagIds,
        @NotNull
        @JsonProperty("excluded_tag_ids") List<@NotNull UUID> excludedTagIds
) {
    public SaveStylePreferencesCommand toCommand(UUID userId) {
        return new SaveStylePreferencesCommand(userId, preferredTagIds, excludedTagIds);
    }
}
