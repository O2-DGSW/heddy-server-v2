package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.port.in.ReauthenticateCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReauthenticateRequest(
        @NotNull ReauthenticateCommand.Method method,
        @Size(max = 100) String password,
        AuthProvider provider,
        @Size(max = 4096) @JsonProperty("provider_token") String providerToken
) {
    public ReauthenticateCommand toCommand(UUID userId) {
        return new ReauthenticateCommand(userId, method, password, provider, providerToken);
    }
}
