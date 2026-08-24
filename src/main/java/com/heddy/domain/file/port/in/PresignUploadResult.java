package com.heddy.domain.file.port.in;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 업로드 세션 발급 결과. 클라이언트는 이 URL 로 스토리지에 직접 올린다. */
public record PresignUploadResult(
        UUID uploadId,
        UUID fileId,
        URI uploadUrl,
        Instant expiresAt
) {
    public PresignUploadResult {
        Objects.requireNonNull(uploadId, "uploadId");
        Objects.requireNonNull(fileId, "fileId");
        Objects.requireNonNull(uploadUrl, "uploadUrl");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
}
