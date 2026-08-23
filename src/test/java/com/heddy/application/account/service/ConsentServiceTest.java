package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.model.ConsentDecision;
import com.heddy.domain.account.model.ConsentSource;
import com.heddy.domain.account.model.ConsentStatus;
import com.heddy.domain.account.model.ConsentType;
import com.heddy.domain.account.port.in.ChangeConsentCommand;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.ConsentHistoryRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    private static final UUID USER_ID = UUID.fromString(
            "80000000-0000-4000-8000-000000000001");
    private static final String CURRENT_POLICY_VERSION = "2026-08-01";

    @Mock AccountRepositoryPort accountRepositoryPort;
    @Mock ConsentHistoryRepositoryPort consentHistoryRepositoryPort;

    private ConsentService service;

    @BeforeEach
    void setUp() {
        service = new ConsentService(
                accountRepositoryPort, consentHistoryRepositoryPort,
                CURRENT_POLICY_VERSION);
    }

    @Test
    void getsLatestConsentStatusesInDeclaredTypeOrder() {
        given(accountRepositoryPort.findById(USER_ID))
                .willReturn(Optional.of(account(AccountStatus.ACTIVE)));
        given(consentHistoryRepositoryPort.findLatestByUserId(USER_ID)).willReturn(List.of(
                status(ConsentType.AI_TRAINING, true, Instant.parse("2026-08-23T02:00:00Z")),
                status(ConsentType.PRIVACY_POLICY, true, Instant.parse("2026-08-23T01:00:00Z"))));

        List<ConsentStatus> result = service.getConsents(USER_ID);

        assertThat(result).extracting(ConsentStatus::type)
                .containsExactly(ConsentType.values());
        assertThat(result.get(0).granted()).isFalse();
        assertThat(result.get(0).policyVersion()).isNull();
        assertThat(result.get(1).granted()).isTrue();
        assertThat(result.get(2).granted()).isTrue();
    }

    @Test
    void appendsHistoryWithCommandSourceAndCurrentPolicyVersion() {
        givenActiveAccount();

        ConsentStatus result = service.changeConsent(new ChangeConsentCommand(
                USER_ID, ConsentType.AI_TRAINING, true,
                CURRENT_POLICY_VERSION, ConsentSource.WITHDRAWAL));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ConsentDecision>> decisions = ArgumentCaptor.forClass(List.class);
        verify(consentHistoryRepositoryPort).append(
                eq(USER_ID), decisions.capture(), eq(ConsentSource.WITHDRAWAL), any());
        assertThat(decisions.getValue()).containsExactly(new ConsentDecision(
                ConsentType.AI_TRAINING, true, CURRENT_POLICY_VERSION));
        assertThat(result.type()).isEqualTo(ConsentType.AI_TRAINING);
        assertThat(result.source()).isEqualTo(ConsentSource.WITHDRAWAL);
    }

    @Test
    void recordsActualCurrentTimeWithoutLockingAccountOrReadingPriorHistory() {
        givenActiveAccount();
        Instant before = Instant.now();

        service.changeConsent(new ChangeConsentCommand(
                USER_ID, ConsentType.SERVICE_ANALYTICS, true,
                CURRENT_POLICY_VERSION, ConsentSource.SETTINGS));
        Instant after = Instant.now();

        ArgumentCaptor<Instant> changedAt = ArgumentCaptor.forClass(Instant.class);
        verify(consentHistoryRepositoryPort).append(
                eq(USER_ID), any(), eq(ConsentSource.SETTINGS), changedAt.capture());
        assertThat(changedAt.getValue()).isBetween(before, after);
        verify(accountRepositoryPort, never()).findByIdForUpdate(any());
    }

    @Test
    void rejectsRequiredConsentWithdrawalAndGuidesAccountDeletion() {
        givenActiveAccount();

        assertAccountError(() -> service.changeConsent(new ChangeConsentCommand(
                        USER_ID, ConsentType.TERMS_OF_SERVICE, false,
                        CURRENT_POLICY_VERSION, ConsentSource.SETTINGS)),
                AccountError.REQUIRED_CONSENT_WITHDRAWAL);

        verifyNoInteractions(consentHistoryRepositoryPort);
    }

    @Test
    void rejectsPolicyVersionThatDiffersFromServerConfiguration() {
        givenActiveAccount();

        assertAccountError(() -> service.changeConsent(new ChangeConsentCommand(
                        USER_ID, ConsentType.AI_TRAINING, true,
                        "2026-07-01", ConsentSource.SETTINGS)),
                AccountError.CONSENT_POLICY_VERSION_INVALID);

        verifyNoInteractions(consentHistoryRepositoryPort);
    }

    @Test
    void rejectsDeletedAccountBeforeReadingConsentHistory() {
        given(accountRepositoryPort.findById(USER_ID))
                .willReturn(Optional.of(account(AccountStatus.DELETED)));

        assertAccountError(() -> service.getConsents(USER_ID), AccountError.ACCOUNT_DELETED);

        verify(consentHistoryRepositoryPort, never()).findLatestByUserId(any());
    }

    private void givenActiveAccount() {
        given(accountRepositoryPort.findById(USER_ID))
                .willReturn(Optional.of(account(AccountStatus.ACTIVE)));
    }

    private ConsentStatus status(ConsentType type, boolean granted, Instant changedAt) {
        return new ConsentStatus(
                USER_ID, type, granted, "2026-08-01", ConsentSource.SIGNUP, changedAt);
    }

    private Account account(AccountStatus status) {
        return new Account(USER_ID, "consent-user@example.com", "hash",
                AuthProvider.EMAIL, null, status, 0, null);
    }

    private void assertAccountError(Runnable action, AccountError expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(AccountException.class,
                        exception -> assertThat(exception.error()).isEqualTo(expected));
    }
}
