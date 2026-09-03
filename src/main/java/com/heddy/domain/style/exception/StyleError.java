package com.heddy.domain.style.exception;

public enum StyleError {
    PREFERENCE_LIMIT_EXCEEDED(
            "STYLE_PREFERENCE_LIMIT_EXCEEDED", "선호 태그와 제외 태그는 각각 최대 10개까지 등록할 수 있습니다."),
    PREFERENCE_CONFLICT(
            "STYLE_PREFERENCE_CONFLICT", "같은 태그를 선호와 제외에 동시에 등록할 수 없습니다."),
    INVALID_TAG_IDS("VALIDATION_FAILED", "존재하지 않는 스타일 태그가 포함되어 있습니다."),
    SAVED_STYLE_DUPLICATED(
            "SAVED_STYLE_DUPLICATED", "이미 저장한 후보 스타일입니다."),
    SAVED_STYLE_LIMIT_EXCEEDED(
            "SAVED_STYLE_LIMIT_EXCEEDED", "후보 스타일은 최대 20개까지 저장할 수 있습니다.");

    private final String code;
    private final String message;

    StyleError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
