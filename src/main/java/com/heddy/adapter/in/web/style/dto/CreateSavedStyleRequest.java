package com.heddy.adapter.in.web.style.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.style.port.in.SavedStyleUseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "추천 결과를 저장 후보 스냅샷으로 보관하는 요청")
public record CreateSavedStyleRequest(
        @NotBlank @Size(max = 100)
        @JsonProperty("style_name") String styleName,

        @NotBlank @Size(max = 2048)
        @JsonProperty("image_url") String imageUrl,

        @NotBlank @Size(max = 500)
        String reason,

        @Size(max = 500)
        String memo
) {
    public SavedStyleUseCase.CreateCommand toCommand(UUID userId) {
        return new SavedStyleUseCase.CreateCommand(
                userId, styleName, imageUrl, reason, memo);
    }
}
