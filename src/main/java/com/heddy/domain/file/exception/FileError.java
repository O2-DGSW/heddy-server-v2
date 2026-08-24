package com.heddy.domain.file.exception;

public enum FileError {
    TOO_LARGE("FILE_TOO_LARGE", "허용된 파일 크기를 초과했습니다."),
    CONTENT_TYPE_NOT_ALLOWED("FILE_CONTENT_TYPE_NOT_ALLOWED", "허용되지 않는 파일 형식입니다."),
    INVALID_STATE_TRANSITION("FILE_INVALID_STATE", "현재 상태에서는 요청한 처리를 할 수 없습니다."),
    CONCURRENT_MODIFICATION("FILE_CONCURRENT_MODIFICATION", "다른 요청이 파일 상태를 먼저 변경했습니다.");

    private final String code;
    private final String message;

    FileError(String code, String message) {
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
