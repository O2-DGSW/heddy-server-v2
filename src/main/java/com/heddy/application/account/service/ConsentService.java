package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.ConsentDecision;
import com.heddy.domain.account.model.ConsentStatus;
import com.heddy.domain.account.model.ConsentType;
import com.heddy.domain.account.port.in.ChangeConsentCommand;
import com.heddy.domain.account.port.in.ConsentUseCase;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.ConsentHistoryRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ConsentService implements ConsentUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final ConsentHistoryRepositoryPort consentHistoryRepositoryPort;
    private final String currentPolicyVersion;

    public ConsentService(
            AccountRepositoryPort accountRepositoryPort,
            ConsentHistoryRepositoryPort consentHistoryRepositoryPort,
            @Value("${app.auth.consent-policy-version}") String currentPolicyVersion
    ) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.consentHistoryRepositoryPort = consentHistoryRepositoryPort;
        this.currentPolicyVersion = currentPolicyVersion;
    }

    @Override
    public List<ConsentStatus> getConsents(UUID userId) {
        validateNonDeletedAccount(userId);
        Map<ConsentType, ConsentStatus> latestByType =
                consentHistoryRepositoryPort.findLatestByUserId(userId).stream()
                        .collect(Collectors.toMap(
                                ConsentStatus::type, Function.identity()));
        return Arrays.stream(ConsentType.values())
                .map(type -> latestByType.getOrDefault(
                        type, ConsentStatus.unrecorded(userId, type)))
                .toList();
    }

    @Override
    @Transactional
    public ConsentStatus changeConsent(ChangeConsentCommand command) {
        validateNonDeletedAccount(command.userId());
        validatePolicyVersion(command.policyVersion());
        validateRequiredConsentWithdrawal(command);

        Instant changedAt = Instant.now();
        ConsentDecision decision = new ConsentDecision(
                command.type(), command.granted(), currentPolicyVersion);
        consentHistoryRepositoryPort.append(
                command.userId(), List.of(decision), command.source(), changedAt);
        return new ConsentStatus(
                command.userId(), decision.type(), decision.granted(),
                decision.policyVersion(), command.source(), changedAt);
    }

    private void validatePolicyVersion(String policyVersion) {
        if (!currentPolicyVersion.equals(policyVersion)) {
            throw new AccountException(AccountError.CONSENT_POLICY_VERSION_INVALID);
        }
    }

    private void validateRequiredConsentWithdrawal(ChangeConsentCommand command) {
        if (command.type().isRequired() && !command.granted()) {
            throw new AccountException(AccountError.REQUIRED_CONSENT_WITHDRAWAL);
        }
    }

    private void validateNonDeletedAccount(UUID userId) {
        validateAccount(accountRepositoryPort.findById(userId)
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND)));
    }

    private void validateAccount(Account account) {
        if (account.isDeleted()) {
            throw new AccountException(AccountError.ACCOUNT_DELETED);
        }
    }
}
