package com.heddy.domain.account.port.in;

public interface EmailLoginUseCase {
    AuthResult login(EmailLoginCommand command);
}
