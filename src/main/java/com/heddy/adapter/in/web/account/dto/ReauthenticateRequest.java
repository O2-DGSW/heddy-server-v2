package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.port.in.ReauthenticateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "민감 작업 재인증 요청. PASSWORD 방식은 password를, "
        + "SOCIAL_TOKEN 방식은 provider와 provider_token을 전달합니다.")
public record ReauthenticateRequest(
        @NotNull ReauthenticateCommand.Method method,
        @Size(max = 100) String password,
        @Schema(description = "소셜 인증 제공자",
                allowableValues = {"GOOGLE", "KAKAO", "APPLE"})
        AuthProvider provider,
        @Size(max = 4096) @JsonProperty("provider_token") String providerToken
) {
    public ReauthenticateCommand toCommand(UUID userId) {
        return new ReauthenticateCommand(userId, method, password, provider, providerToken);
    }
}
