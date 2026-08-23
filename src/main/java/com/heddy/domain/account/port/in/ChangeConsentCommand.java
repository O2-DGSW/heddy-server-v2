package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.ConsentSource;
import com.heddy.domain.account.model.ConsentType;

import java.util.UUID;

public record ChangeConsentCommand(
        UUID userId,
        ConsentType type,
        boolean granted,
        String policyVersion,
        ConsentSource source
) {
}
