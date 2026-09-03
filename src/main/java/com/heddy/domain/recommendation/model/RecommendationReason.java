package com.heddy.domain.recommendation.model;

import java.util.Map;

public record RecommendationReason(Code code, Map<String, String> params) {
    public enum Code {
        SIMILAR_HIGH_SATISFACTION_HISTORY,
        SAVED_STYLE_MATCH,
        PREFERRED_TAG_MATCH,
        HAIR_PROFILE_COMPATIBLE,
        CARE_TIME_FIT,
        EDITORIAL_FALLBACK
    }

    public RecommendationReason {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
