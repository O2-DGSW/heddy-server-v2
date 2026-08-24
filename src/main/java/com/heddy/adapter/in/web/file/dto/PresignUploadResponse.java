package com.heddy.adapter.in.web.file.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.file.port.in.PresignUploadResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Presigned PUT 요청 한 벌. URL 에는 서명이 들어 있으므로 로그에 남지 않게 주의한다.
 */
public record PresignUploadResponse(
        @Schema(description = "업로드 세션 식별자. complete 요청에 쓴다")
        @JsonProperty("upload_id") UUID uploadId,

        @Schema(description = "파일 식별자. READY 가 된 뒤 다른 도메인이 참조한다")
        @JsonProperty("file_id") UUID fileId,

        @Schema(description = "Presigned PUT URL. required_headers 를 빠뜨리지 말고 함께 보낸다")
        @JsonProperty("upload_url") String uploadUrl,

        @Schema(description = "HTTP 메서드. 항상 PUT 이다")
        @JsonProperty("method") String method,

        @Schema(description = "업로드 URL 호출 때 반드시 보내야 하는 헤더. If-None-Match:* 는 같은 키 객체의 "
                + "덮어쓰기를 막는 조건으로, 서명에 포함돼 있어 누락하면 스토리지가 거부한다")
        @JsonProperty("required_headers") Map<String, String> requiredHeaders,

        @Schema(description = "세션 만료 시각. URL 도 이 시각까지만 유효하다")
        @JsonProperty("expires_at") Instant expiresAt
) {
    public static PresignUploadResponse from(PresignUploadResult result) {
        return new PresignUploadResponse(
                result.uploadId(), result.fileId(), result.upload().url().toString(),
                result.upload().method(), result.upload().requiredHeaders(), result.expiresAt());
    }
}
