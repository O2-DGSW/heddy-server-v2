package com.heddy.domain.recommendation.model;

import java.util.List;
import java.util.UUID;

public record ScoredRecommendation(
        HairstyleCandidate candidate,
        ScoreBreakdown scoreBreakdown,
        List<RecommendationReason> reasons,
        UUID referenceRecordId
) {
    public ScoredRecommendation {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public double finalScore() {
        return scoreBreakdown.finalScore();
    }
}
