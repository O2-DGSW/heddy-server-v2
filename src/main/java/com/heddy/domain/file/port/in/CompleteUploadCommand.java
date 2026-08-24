package com.heddy.domain.file.port.in;

import java.util.Objects;
import java.util.UUID;

/** 업로드 완료 입력. 세션 소유자와 인증 사용자의 일치는 서비스가 확인한다. */
public record CompleteUploadCommand(
        UUID userId,
        UUID uploadId
) {
    public CompleteUploadCommand {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(uploadId, "uploadId");
    }
}
