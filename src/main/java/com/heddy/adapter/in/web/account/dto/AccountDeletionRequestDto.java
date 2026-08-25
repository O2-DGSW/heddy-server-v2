package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.port.in.RequestAccountDeletionUseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AccountDeletionRequestDto(
        @NotBlank
        @JsonProperty("reauthentication_token")
        @Schema(description = "POST /auth/reauthenticate에서 발급한 300초 유효 1회용 토큰")
        String reauthenticationToken,

        @Size(max = 255)
        @Schema(description = "탈퇴 사유. 선택 입력, 최대 255자")
        String reason
) {
    public RequestAccountDeletionUseCase.Command toCommand(UUID userId) {
        return new RequestAccountDeletionUseCase.Command(userId, reauthenticationToken, reason);
    }
}
