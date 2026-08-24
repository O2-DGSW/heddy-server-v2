package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.AccountStatus;

import java.util.UUID;

public record AuthUser(UUID userId, String email, String nickname, AccountStatus status) {
}
