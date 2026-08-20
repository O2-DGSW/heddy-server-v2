package com.heddy.domain.account.port.in;

public interface RefreshTokenUseCase {
    AuthTokens refresh(String refreshToken);
}
