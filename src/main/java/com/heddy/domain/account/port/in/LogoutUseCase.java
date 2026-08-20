package com.heddy.domain.account.port.in;

import java.util.UUID;

public interface LogoutUseCase {
    void logout(UUID userId, String refreshToken);
}
