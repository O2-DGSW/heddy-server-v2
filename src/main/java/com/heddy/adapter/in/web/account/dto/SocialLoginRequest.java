package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.port.in.SocialLoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "소셜 로그인 요청")
public record SocialLoginRequest(
        @NotNull
        @Schema(description = "소셜 인증 제공자",
                allowableValues = {"GOOGLE", "KAKAO", "APPLE"})
        AuthProvider provider,
        @NotBlank @Size(max = 4096) @JsonProperty("provider_token") String providerToken
) {
    public SocialLoginCommand toCommand() {
        return new SocialLoginCommand(provider, providerToken);
    }
}
