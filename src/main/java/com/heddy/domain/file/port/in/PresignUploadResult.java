package com.heddy.domain.file.port.in;

import com.heddy.domain.file.model.PresignedUpload;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 업로드 세션 발급 결과. 클라이언트는 결과의 URL 과 필수 헤더로 스토리지에 직접 올린다.
 *
 * <p>{@code upload} 는 서명된 요청 한 벌이다. 조건부 헤더({@code If-None-Match})까지 서명에
 * 들어가므로 URL 만으로는 업로드할 수 없고, 어떤 헤더를 보내야 하는지도 이 결과가 안다.
 */
public record PresignUploadResult(
        UUID uploadId,
        UUID fileId,
        PresignedUpload upload,
        Instant expiresAt
) {
    public PresignUploadResult {
        Objects.requireNonNull(uploadId, "uploadId");
        Objects.requireNonNull(fileId, "fileId");
        Objects.requireNonNull(upload, "upload");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
}
