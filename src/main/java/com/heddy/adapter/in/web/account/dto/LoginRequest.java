package com.heddy.adapter.in.web.account.dto;

import com.heddy.domain.account.port.in.LoginCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 20) String loginId,
        @NotBlank @Size(max = 100) String password
) {

    public LoginCommand toCommand() {
        return new LoginCommand(loginId, password);
    }
}
