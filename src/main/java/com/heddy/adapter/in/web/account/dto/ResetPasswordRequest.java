package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.port.in.ResetPasswordCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Pattern(regexp = "^01[016789]\\d{7,8}$") @JsonProperty("phone_number") String phoneNumber,
        @NotBlank @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*?_~])[a-zA-Z0-9!@#$%^&*?_~]+$")
        @JsonProperty("new_password") String newPassword
) {
    public ResetPasswordCommand toCommand() {
        return new ResetPasswordCommand(phoneNumber, newPassword);
    }
}
