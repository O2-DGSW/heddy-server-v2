package com.heddy.adapter.in.web.file.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.file.port.in.CompleteUploadResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record CompleteUploadResponse(
        @Schema(description = "파일 식별자. 다른 도메인은 READY 인 파일만 참조할 수 있다")
        @JsonProperty("file_id") UUID fileId,

        @JsonProperty("upload_id") UUID uploadId,

        @Schema(description = "전이 결과 상태. 항상 READY 다")
        @JsonProperty("status") String status,

        @JsonProperty("content_type") String contentType,

        @Schema(description = "스토리지에서 확인한 실제 크기(바이트)")
        @JsonProperty("file_size") long fileSize
) {
    public static CompleteUploadResponse from(CompleteUploadResult result) {
        return new CompleteUploadResponse(
                result.fileId(), result.uploadId(), result.status().name(),
                result.contentType(), result.fileSize());
    }
}
