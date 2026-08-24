package com.heddy.domain.file.exception;

public enum FileError {
    TOO_LARGE("FILE_TOO_LARGE", "허용된 파일 크기를 초과했습니다."),
    CONTENT_TYPE_NOT_ALLOWED("FILE_CONTENT_TYPE_NOT_ALLOWED", "허용되지 않는 파일 형식입니다."),
    PURPOSE_NOT_ALLOWED("FILE_PURPOSE_NOT_ALLOWED", "외부에서 요청할 수 없는 파일 용도입니다."),
    CONTENT_TYPE_MISMATCH("FILE_CONTENT_TYPE_MISMATCH", "객체의 Content-Type 이 업로드 세션과 일치하지 않습니다."),
    SIZE_MISMATCH("FILE_SIZE_MISMATCH", "객체의 크기가 업로드 세션에 선언한 크기와 일치하지 않습니다."),
    OBJECT_NOT_FOUND("FILE_OBJECT_NOT_FOUND", "업로드된 객체를 찾을 수 없습니다."),
    UPLOAD_EXPIRED("FILE_UPLOAD_EXPIRED", "만료된 업로드 세션입니다."),
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
