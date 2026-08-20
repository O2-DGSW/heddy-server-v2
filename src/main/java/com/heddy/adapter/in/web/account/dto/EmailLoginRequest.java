package com.heddy.adapter.in.web.account.dto;

import com.heddy.domain.account.port.in.EmailLoginCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmailLoginRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String password,
        @NotNull @Valid DeviceRequest device
) {
    public EmailLoginCommand toCommand() {
        return new EmailLoginCommand(email.toLowerCase(), password, device.toDomain());
    }
}
