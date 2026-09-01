package com.heddy.domain.recommendation.model;

import com.heddy.domain.recommendation.model.HairstyleCandidate.ManagementDifficulty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RecommendationItem(
        UUID recommendationItemId,
        UUID hairstyleId,
        UUID colorId,
        int displayRank,
        BigDecimal score,
        ScoreBreakdown scoreBreakdown,
        List<RecommendationReason> reasons,
        ManagementDifficulty managementDifficulty,
        int estimatedDailyCareMinutes,
        RecommendationReference referenceRecord
) {
    public RecommendationItem {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
