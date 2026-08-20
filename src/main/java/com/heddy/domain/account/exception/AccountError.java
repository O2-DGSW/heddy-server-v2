package com.heddy.domain.account.exception;

public enum AccountError {
    EMAIL_ALREADY_EXISTS("AUTH_EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED("AUTH_ACCOUNT_LOCKED", "로그인 시도 횟수를 초과해 계정이 잠겼습니다."),
    ACCOUNT_DELETED("AUTH_ACCOUNT_DELETED", "탈퇴했거나 삭제 대기 중인 계정입니다."),
    SOCIAL_TOKEN_INVALID("AUTH_SOCIAL_TOKEN_INVALID", "소셜 인증 토큰이 올바르지 않습니다."),
    SOCIAL_ACCOUNT_ALREADY_LINKED("AUTH_SOCIAL_ACCOUNT_ALREADY_LINKED", "이미 가입된 소셜 계정입니다."),
    REFRESH_TOKEN_INVALID("AUTH_REFRESH_TOKEN_INVALID", "Refresh Token이 올바르지 않습니다."),
    REFRESH_TOKEN_REUSED("AUTH_REFRESH_TOKEN_REUSED", "이미 사용된 Refresh Token입니다."),
    REAUTHENTICATION_REQUIRED("AUTH_REAUTHENTICATION_REQUIRED", "민감 작업을 위한 재인증이 필요합니다."),
    WEAK_PASSWORD("AUTH_WEAK_PASSWORD", "비밀번호 형식이 올바르지 않습니다."),
    CONSENT_REQUIRED_NOT_GRANTED("CONSENT_REQUIRED_NOT_GRANTED", "필수 약관에 동의해야 합니다."),
    ACCOUNT_NOT_FOUND("RESOURCE_NOT_FOUND", "계정을 찾을 수 없습니다."),
    PHONE_ALREADY_EXISTS("AUTH_PHONE_ALREADY_EXISTS", "이미 사용 중인 전화번호입니다."),
    PROFILE_INVALID_NICKNAME("VALIDATION_FAILED", "닉네임은 비어 있을 수 없습니다."),
    HAIR_PROFILE_NOT_FOUND("HAIR_PROFILE_NOT_FOUND", "모발 프로필을 찾을 수 없습니다."),
    PHONE_NOT_VERIFIED("AUTH_PHONE_NOT_VERIFIED", "전화번호 인증이 완료되지 않았습니다."),
    SMS_CODE_NOT_FOUND("AUTH_SMS_CODE_NOT_FOUND", "인증 코드를 찾을 수 없습니다."),
    SMS_CODE_INVALID("AUTH_SMS_CODE_INVALID", "인증 코드가 올바르지 않습니다."),
    SMS_CODE_MAX_ATTEMPTS("AUTH_SMS_CODE_MAX_ATTEMPTS", "인증 시도 횟수를 초과했습니다."),
    SMS_SEND_TOO_SOON("AUTH_SMS_SEND_TOO_SOON", "잠시 후 다시 시도해주세요."),
    SMS_SEND_FAILED("AUTH_SMS_SEND_FAILED", "인증번호 발송에 실패했습니다.");

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
