package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.ConsentDecision;
import com.heddy.domain.account.model.ConsentSource;
import com.heddy.domain.account.model.ConsentStatus;
import com.heddy.domain.account.port.in.ChangeConsentCommand;
import com.heddy.domain.account.port.in.ConsentUseCase;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.ConsentHistoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsentService implements ConsentUseCase {

    private static final long POSTGRES_TIMESTAMP_PRECISION_NANOS = 1_000L;

    private final AccountRepositoryPort accountRepositoryPort;
    private final ConsentHistoryRepositoryPort consentHistoryRepositoryPort;

    @Override
    public List<ConsentStatus> getConsents(UUID userId) {
        validateNonDeletedAccount(userId);
        return consentHistoryRepositoryPort.findLatestByUserId(userId).stream()
                .sorted(Comparator.comparingInt(status -> status.type().ordinal()))
                .toList();
    }

    @Override
    @Transactional
    public ConsentStatus changeConsent(ChangeConsentCommand command) {
        lockAndValidateNonDeletedAccount(command.userId());
        validatePolicyVersion(command.policyVersion());
        validateRequiredConsentWithdrawal(command);

        Instant changedAt = nextChangedAt(command);
        ConsentDecision decision = new ConsentDecision(
                command.type(), command.granted(), command.policyVersion().trim());
        consentHistoryRepositoryPort.append(
                command.userId(), List.of(decision), ConsentSource.SETTINGS, changedAt);
        return new ConsentStatus(
                command.userId(), decision.type(), decision.granted(),
                decision.policyVersion(), ConsentSource.SETTINGS, changedAt);
    }

    private Instant nextChangedAt(ChangeConsentCommand command) {
        Instant now = Instant.now();
        return consentHistoryRepositoryPort
                .findLatestByUserIdAndType(command.userId(), command.type())
                .map(ConsentStatus::changedAt)
                .map(previous -> laterOf(
                        now, previous.plusNanos(POSTGRES_TIMESTAMP_PRECISION_NANOS)))
                .orElse(now);
    }

    private Instant laterOf(Instant first, Instant second) {
        return first.isAfter(second) ? first : second;
    }

    private void validatePolicyVersion(String policyVersion) {
        if (policyVersion == null || policyVersion.isBlank()) {
            throw new AccountException(AccountError.CONSENT_POLICY_VERSION_REQUIRED);
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

    private void lockAndValidateNonDeletedAccount(UUID userId) {
        validateAccount(accountRepositoryPort.findByIdForUpdate(userId)
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND)));
    }

    private void validateAccount(Account account) {
        if (account.isDeleted()) {
            throw new AccountException(AccountError.ACCOUNT_DELETED);
        }
    }
}
