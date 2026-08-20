package com.heddy.domain.account.model;

public record ConsentDecision(
        ConsentType type,
        boolean granted,
        String policyVersion
) {
}
