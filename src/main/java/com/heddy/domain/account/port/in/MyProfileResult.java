package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record MyProfileResult(
        UUID userId,
        String email,
        String nickname,
        String phone,
        String preferredDesigner,
        String hairCautions,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
