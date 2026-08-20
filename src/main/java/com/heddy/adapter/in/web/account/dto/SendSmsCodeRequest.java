package com.heddy.adapter.in.web.account.dto;

import com.heddy.domain.account.model.SmsVerificationPurpose;
import com.heddy.domain.account.port.in.SendSmsCodeCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendSmsCodeRequest(
        @NotBlank @Pattern(regexp = "^01[016789]\\d{7,8}$") String phoneNumber,
        @NotBlank @Size(max = 20) String carrier,
        @NotNull SmsVerificationPurpose purpose
) {
    public SendSmsCodeCommand toCommand() {
        return new SendSmsCodeCommand(phoneNumber, carrier, purpose);
    }
}
