package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.ConsentDecision;
import com.heddy.domain.account.model.ConsentType;

import java.util.Arrays;
import java.util.List;

final class ConsentValidator {

    private ConsentValidator() {
    }

    static void requireSignupConsents(List<ConsentDecision> agreements) {
        boolean allRequiredGranted = Arrays.stream(ConsentType.values())
                .filter(ConsentType::isRequired)
                .allMatch(type -> isGranted(agreements, type));
        if (!allRequiredGranted) {
            throw new AccountException(AccountError.CONSENT_REQUIRED_NOT_GRANTED);
        }
    }

    private static boolean isGranted(List<ConsentDecision> agreements, ConsentType type) {
        return agreements.stream().anyMatch(decision -> decision.type() == type && decision.granted());
    }
}
