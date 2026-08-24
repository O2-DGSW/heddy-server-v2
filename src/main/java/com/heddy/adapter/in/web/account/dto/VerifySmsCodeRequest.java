package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.SmsVerificationPurpose;
import com.heddy.domain.account.port.in.VerifySmsCodeCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifySmsCodeRequest(
        @NotBlank @Pattern(regexp = "^01[016789]\\d{7,8}$") @JsonProperty("phone_number") String phoneNumber,
        @NotBlank @Size(min = 6, max = 6) String code,
        @NotNull SmsVerificationPurpose purpose
) {
    public VerifySmsCodeCommand toCommand() {
        return new VerifySmsCodeCommand(phoneNumber, code, purpose);
    }
}
