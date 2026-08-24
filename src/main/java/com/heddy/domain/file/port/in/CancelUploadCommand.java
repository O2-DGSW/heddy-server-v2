package com.heddy.domain.file.port.in;

import java.util.Objects;
import java.util.UUID;

/** 업로드 취소 입력. 세션 소유자와 인증 사용자의 일치는 서비스가 확인한다. */
public record CancelUploadCommand(
        UUID userId,
        UUID uploadId
) {
    public CancelUploadCommand {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(uploadId, "uploadId");
    }
}
