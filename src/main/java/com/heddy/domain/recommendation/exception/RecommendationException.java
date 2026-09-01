package com.heddy.domain.recommendation.exception;

public class RecommendationException extends RuntimeException {
    private final RecommendationError error;

    public RecommendationException(RecommendationError error) {
        super(error.message());
        this.error = error;
    }

    public RecommendationError error() { return error; }
}
