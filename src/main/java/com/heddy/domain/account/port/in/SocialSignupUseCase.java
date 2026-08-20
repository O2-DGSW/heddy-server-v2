package com.heddy.domain.account.port.in;

public interface SocialSignupUseCase {
    AuthTokens signup(SocialSignupCommand command);
}
