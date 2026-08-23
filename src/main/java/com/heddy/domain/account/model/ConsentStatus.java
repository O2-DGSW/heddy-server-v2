package com.heddy.domain.account.model;

import java.time.Instant;
import java.util.UUID;

public record ConsentStatus(
        UUID userId,
        ConsentType type,
        boolean granted,
        String policyVersion,
        ConsentSource source,
        Instant changedAt
) {
}
