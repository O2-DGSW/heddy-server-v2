package com.heddy.domain.recommendation.model;

public record ScoreBreakdown(
        double historyScore,
        double savedStyleScore,
        double preferredTagScore,
        double hairCompatibilityScore,
        double careTimeScore,
        double rawScore,
        double availableMaximumScore,
        double finalScore,
        double photoSimilarityScore
) {
    public ScoreBreakdown(double history, double saved, double preferred, double hair, double care,
            double raw, double maximum, double score) {
        this(history, saved, preferred, hair, care, raw, maximum, score, 0);
    }
}
