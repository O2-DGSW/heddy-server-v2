package com.heddy.adapter.in.web.file.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.file.port.in.PresignUploadResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Presigned PUT URL 을 담는다. URL 에는 서명이 들어 있으므로 로그에 남지 않게 주의한다.
 */
public record PresignUploadResponse(
        @Schema(description = "업로드 세션 식별자. complete 요청에 쓴다")
        @JsonProperty("upload_id") UUID uploadId,

        @Schema(description = "파일 식별자. READY 가 된 뒤 다른 도메인이 참조한다")
        @JsonProperty("file_id") UUID fileId,

        @Schema(description = "Presigned PUT URL. 이 URL 로 Content-Type 을 맞춰 직접 올린다")
        @JsonProperty("upload_url") URI uploadUrl,

        @Schema(description = "세션 만료 시각. URL 도 이 시각까지만 유효하다")
        @JsonProperty("expires_at") Instant expiresAt
) {
    public static PresignUploadResponse from(PresignUploadResult result) {
        return new PresignUploadResponse(
                result.uploadId(), result.fileId(), result.uploadUrl(), result.expiresAt());
    }
}
