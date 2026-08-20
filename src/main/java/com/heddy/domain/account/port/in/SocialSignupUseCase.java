package com.heddy.domain.account.port.in;

public interface SocialSignupUseCase {
    AuthResult signup(SocialSignupCommand command);
}
