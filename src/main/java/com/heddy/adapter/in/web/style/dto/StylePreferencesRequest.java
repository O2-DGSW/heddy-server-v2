package com.heddy.adapter.in.web.style.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.style.port.in.SaveStylePreferencesCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record StylePreferencesRequest(
        @NotNull @Size(max = 10) @Valid
        @JsonProperty("preferred_tag_ids") List<@NotNull UUID> preferredTagIds,
        @NotNull @Size(max = 10) @Valid
        @JsonProperty("excluded_tag_ids") List<@NotNull UUID> excludedTagIds
) {
    public SaveStylePreferencesCommand toCommand(UUID userId) {
        return new SaveStylePreferencesCommand(userId, preferredTagIds, excludedTagIds);
    }
}
