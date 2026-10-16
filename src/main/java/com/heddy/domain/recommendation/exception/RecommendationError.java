package com.heddy.domain.recommendation.exception;

public enum RecommendationError {
    HAIR_LENGTH_REQUIRED("RECOMMENDATION_HAIR_LENGTH_REQUIRED", "사진에서 현재 머리 길이를 확인할 수 없습니다. 프로필의 현재 머리 길이를 입력해 주세요."),
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
