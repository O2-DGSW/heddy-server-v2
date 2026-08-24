package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.ConsentSource;
import com.heddy.domain.account.model.ConsentType;
import com.heddy.domain.account.port.in.ChangeConsentCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChangeConsentRequest(
        @NotNull
        @Schema(description = "동의 여부")
        Boolean granted,
        @NotBlank
        @Size(max = 20)
        @Schema(description = "사용자에게 고지한 정책 버전", example = "2026-08-01")
        @JsonProperty("policy_version") String policyVersion
) {
    public ChangeConsentCommand toCommand(UUID userId, ConsentType type) {
        return new ChangeConsentCommand(
                userId, type, granted, policyVersion, ConsentSource.SETTINGS);
    }
}
