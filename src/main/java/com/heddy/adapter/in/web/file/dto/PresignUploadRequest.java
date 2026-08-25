package com.heddy.adapter.in.web.file.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.port.in.PresignUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PresignUploadRequest(
        @NotNull
        @Schema(description = "파일 용도. TREATMENT_PHOTO(최대 10MB, image/jpeg·png·heic) / "
                + "AR_CAPTURE(최대 5MB, image/jpeg·png). "
                + "ANALYSIS_OVERLAY_INTERNAL 은 분석 서버 전용이라 외부 발급이 거부된다")
        @JsonProperty("purpose") FilePurpose purpose,

        @NotBlank
        @Schema(description = "업로드할 파일의 Content-Type. 용도별 허용 목록에 있어야 하고, 이 값으로 PUT 이 서명된다")
        @JsonProperty("content_type") String contentType,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "원본 파일명. 오브젝트 키 생성에는 쓰이지 않고 감사·표시 목적으로만 보관된다")
        @JsonProperty("file_name") String fileName,

        @Positive
        @Schema(description = "선언 크기(바이트). 용도별 최대치를 넘으면 발급이 거부되고, 완료 시점에 "
                + "실측 크기와 일치하는지 재검증된다")
        @JsonProperty("file_size") long fileSize,

        @NotBlank
        @Pattern(regexp = "^[0-9a-fA-F]{64}$")
        @Schema(description = "파일의 SHA-256 해시(64자리 헥사). 클라이언트 선언값으로 기록되며, 내용 검증 단계에서 실측값으로 대체된다")
        @JsonProperty("sha256") String sha256
) {
    public PresignUploadCommand toCommand(UUID userId) {
        return new PresignUploadCommand(userId, purpose, contentType, fileName, fileSize, sha256);
    }
}
