package com.heddy.adapter.in.web.account.dto;

import com.heddy.domain.account.port.in.SocialSignupCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SocialSignupRequest(
        @NotBlank @Size(max = 100) String pendingToken,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "^01[016789]\\d{7,8}$") String phoneNumber
) {
    public SocialSignupCommand toCommand() {
        return new SocialSignupCommand(pendingToken, name, phoneNumber);
    }
}
