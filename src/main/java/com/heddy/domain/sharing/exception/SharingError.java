package com.heddy.domain.sharing.exception;

public enum SharingError {
    EMPTY_SELECTION("SHARE_EMPTY_SELECTION", "공유할 기록 또는 후보 스타일과 항목을 1개 이상 선택해야 합니다."),
    TOKEN_HASH_REQUIRED("SHARING_TOKEN_HASH_REQUIRED", "공유 토큰 해시는 필수입니다."),
    EXPIRES_AT_REQUIRED("SHARING_EXPIRES_AT_REQUIRED", "공유 만료 시각은 필수입니다."),
    EXPIRES_AT_NOT_FUTURE("SHARING_EXPIRES_AT_NOT_FUTURE", "공유 만료 시각은 미래여야 합니다."),
    EXPIRES_IN_DAYS_INVALID("SHARING_EXPIRES_IN_DAYS_INVALID", "유효기간은 1일 이상이어야 합니다."),
    FIELD_UNKNOWN("SHARING_FIELD_UNKNOWN", "저장된 공유 항목 값을 읽을 수 없습니다.");

    private final String code;
    private final String message;

    SharingError(String code, String message) {
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
