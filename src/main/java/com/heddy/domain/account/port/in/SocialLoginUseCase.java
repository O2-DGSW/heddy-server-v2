package com.heddy.domain.account.port.in;

public interface SocialLoginUseCase {
    AuthResult login(SocialLoginCommand command);
}
