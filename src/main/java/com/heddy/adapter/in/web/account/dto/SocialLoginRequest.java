package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.port.in.SocialLoginCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SocialLoginRequest(
        @NotNull AuthProvider provider,
        @NotBlank @Size(max = 4096) @JsonProperty("provider_token") String providerToken,
        @NotNull @Valid DeviceRequest device
) {
    public SocialLoginCommand toCommand() {
        return new SocialLoginCommand(provider, providerToken, device.toDomain());
    }
}
