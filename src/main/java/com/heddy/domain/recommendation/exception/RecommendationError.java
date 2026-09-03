package com.heddy.domain.recommendation.exception;

public enum RecommendationError {
    NOT_FOUND("RECOMMENDATION_NOT_FOUND", "추천 결과를 찾을 수 없습니다."),
    NO_ELIGIBLE_CANDIDATES("RECOMMENDATION_CANDIDATES_UNAVAILABLE", "추천 가능한 헤어스타일이 없습니다.");

    private final String code;
    private final String message;

    RecommendationError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() { return code; }
    public String message() { return message; }
}
