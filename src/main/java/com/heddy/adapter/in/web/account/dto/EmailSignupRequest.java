package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.port.in.EmailSignupCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record EmailSignupRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 30) String nickname,
        @Pattern(regexp = "^01[016789]\\d{7,8}$") @JsonProperty("phone_number") String phoneNumber,
        @NotNull @Valid AgreementsRequest agreements
) {
    public EmailSignupCommand toCommand(String policyVersion) {
        return new EmailSignupCommand(
                email.toLowerCase(), password, nickname, phoneNumber,
                agreements.toDecisions(policyVersion));
    }
}
