package com.heddy.domain.account.port.in;

public interface ReauthenticateUseCase {
    ReauthenticateResult reauthenticate(ReauthenticateCommand command);
}
