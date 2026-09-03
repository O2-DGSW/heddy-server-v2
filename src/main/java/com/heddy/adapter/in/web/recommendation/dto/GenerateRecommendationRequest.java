package com.heddy.adapter.in.web.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GenerateRecommendationRequest(
        @JsonProperty("force_refresh") Boolean forceRefresh
) {
    public boolean shouldForceRefresh() {
        return Boolean.TRUE.equals(forceRefresh);
    }
}
