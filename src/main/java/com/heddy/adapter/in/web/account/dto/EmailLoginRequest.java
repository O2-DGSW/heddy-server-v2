package com.heddy.adapter.in.web.account.dto;

import com.heddy.domain.account.port.in.EmailLoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "이메일 로그인 요청")
public record EmailLoginRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String password,
        @NotNull @Valid
        @Schema(description = "발급되는 Refresh Token 세션에 기록할 기기 정보")
        DeviceRequest device
) {
    public EmailLoginCommand toCommand() {
        return new EmailLoginCommand(email.toLowerCase(), password, device.toDomain());
    }
}
