package com.heddy.domain.account.port.in;

public interface SendSmsCodeUseCase {
    void send(SendSmsCodeCommand command);
}
