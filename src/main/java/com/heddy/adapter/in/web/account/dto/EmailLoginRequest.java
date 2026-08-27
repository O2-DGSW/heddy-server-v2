package com.heddy.adapter.in.web.account.dto;

import com.heddy.domain.account.port.in.EmailLoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "이메일 로그인 요청")
public record EmailLoginRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String password
) {
    public EmailLoginCommand toCommand() {
        return new EmailLoginCommand(email.toLowerCase(), password);
    }
}
