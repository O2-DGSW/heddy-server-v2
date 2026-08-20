package com.heddy.domain.account.exception;

public enum AccountError {
    LOGIN_ID_DUPLICATED("AUTH_001", "이미 사용 중인 아이디입니다."),
    PHONE_DUPLICATED("AUTH_002", "이미 사용 중인 전화번호입니다."),
    LOGIN_FAILED("AUTH_003", "아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_SUSPENDED("AUTH_004", "정지된 계정입니다."),
    ACCOUNT_INACTIVE("AUTH_005", "탈퇴한 계정입니다."),
    INVALID_REFRESH_TOKEN("AUTH_006", "유효하지 않은 리프레시 토큰입니다."),
    PHONE_NOT_VERIFIED("AUTH_007", "전화번호 인증이 완료되지 않았습니다."),
    SOCIAL_PENDING_EXPIRED("AUTH_008", "소셜 인증이 만료되었습니다."),
    SOCIAL_ALREADY_LINKED("AUTH_009", "이미 가입된 소셜 계정입니다."),
    ACCOUNT_NOT_FOUND("AUTH_010", "계정을 찾을 수 없습니다."),
    SMS_CODE_NOT_FOUND("AUTH_011", "인증 코드를 찾을 수 없습니다."),
    SMS_CODE_INVALID("AUTH_012", "인증 코드가 올바르지 않습니다."),
    SMS_CODE_MAX_ATTEMPTS("AUTH_013", "인증 시도 횟수를 초과했습니다."),
    SMS_SEND_TOO_SOON("AUTH_014", "잠시 후 다시 시도해주세요."),
    SMS_SEND_FAILED("AUTH_015", "인증번호 발송에 실패했습니다.");

    private final String code;
    private final String message;

    AccountError(String code, String message) {
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
