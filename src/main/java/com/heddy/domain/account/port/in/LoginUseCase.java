package com.heddy.domain.account.port.in;

public interface LoginUseCase {
    AuthTokens login(LoginCommand command);
}
