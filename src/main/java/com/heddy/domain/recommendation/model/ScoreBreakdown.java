package com.heddy.domain.recommendation.model;

public record ScoreBreakdown(
        double historyScore,
        double savedStyleScore,
        double preferredTagScore,
        double hairCompatibilityScore,
        double careTimeScore,
        double rawScore,
        double availableMaximumScore,
        double finalScore
) {
}
