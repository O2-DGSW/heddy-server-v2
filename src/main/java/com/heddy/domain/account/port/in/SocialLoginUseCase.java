package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.Account;

public interface SocialLoginUseCase {
    AuthTokens login(Account account);
}
