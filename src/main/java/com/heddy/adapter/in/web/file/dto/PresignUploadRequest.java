package com.heddy.adapter.in.web.file.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.port.in.PresignUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record PresignUploadRequest(
        @NotNull
        @Schema(description = "파일 용도. TREATMENT_PHOTO(최대 10MB, image/jpeg·png·heic) / "
                + "AR_CAPTURE(최대 5MB, image/jpeg·png) / "
                + "ANALYSIS_OVERLAY_INTERNAL(최대 5MB, image/png)")
        @JsonProperty("purpose") FilePurpose purpose,

        @NotBlank
        @Schema(description = "업로드할 파일의 Content-Type. 용도별 허용 목록에 있어야 하고, 이 값으로 PUT URL 이 서명된다")
        @JsonProperty("content_type") String contentType,

        @Positive
        @Schema(description = "선언 크기(바이트). 용도별 최대치를 넘으면 발급이 거부되며, 실제 크기는 완료 시점에 재검증된다")
        @JsonProperty("file_size") long fileSize
) {
    public PresignUploadCommand toCommand(UUID userId) {
        return new PresignUploadCommand(userId, purpose, contentType, fileSize);
    }
}
