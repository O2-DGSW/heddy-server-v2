package com.heddy.application.account.service;

import com.heddy.domain.account.port.in.LogoutUseCase;
import com.heddy.domain.account.port.out.RefreshTokenStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenStorePort refreshTokenStorePort;

    @Override
    public void logout(Long accountId) {
        refreshTokenStorePort.delete(accountId);
    }
}
