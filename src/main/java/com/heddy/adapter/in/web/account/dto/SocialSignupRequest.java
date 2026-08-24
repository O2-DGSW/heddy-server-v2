package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.port.in.SocialSignupCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record SocialSignupRequest(
        @NotNull AuthProvider provider,
        @NotBlank @Size(max = 4096) @JsonProperty("provider_token") String providerToken,
        @NotBlank @Size(max = 30) String nickname,
        @Pattern(regexp = "^01[016789]\\d{7,8}$") @JsonProperty("phone_number") String phoneNumber,
        @NotNull @Valid AgreementsRequest agreements
) {
    public SocialSignupCommand toCommand(String policyVersion) {
        return new SocialSignupCommand(
                provider, providerToken, nickname, phoneNumber,
                agreements.toDecisions(policyVersion));
    }
}
