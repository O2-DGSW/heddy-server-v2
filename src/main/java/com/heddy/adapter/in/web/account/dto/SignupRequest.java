package com.heddy.adapter.in.web.account.dto;

import com.heddy.domain.account.port.in.SignupAccountCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(min = 4, max = 20) String loginId,
        @NotBlank @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*?_~])[a-zA-Z0-9!@#$%^&*?_~]+$")
        String password,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "^01[016789]\\d{7,8}$") String phoneNumber
) {
    public SignupAccountCommand toCommand() {
        return new SignupAccountCommand(loginId, password, name, phoneNumber);
    }
}
