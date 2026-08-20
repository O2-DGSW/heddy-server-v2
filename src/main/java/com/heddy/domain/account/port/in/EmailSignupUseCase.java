package com.heddy.domain.account.port.in;

public interface EmailSignupUseCase {
    AuthResult signup(EmailSignupCommand command);
}
