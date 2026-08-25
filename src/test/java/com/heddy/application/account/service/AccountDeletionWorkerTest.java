package com.heddy.application.account.service;

import com.heddy.domain.account.model.AccountDeletionRequest;
import com.heddy.domain.account.model.AccountDeletionStatus;
import com.heddy.domain.account.port.out.AccountDeletionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountDeletionWorkerTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock AccountDeletionRepositoryPort deletionRepositoryPort;
    @Mock AccountDeletionProcessor processor;

    private AccountDeletionWorker worker;

    @BeforeEach
    void setUp() {
        worker = new AccountDeletionWorker(
                deletionRepositoryPort, processor, Clock.fixed(NOW, ZoneOffset.UTC), 5, 300);
    }

    @Test
    void requeuesFailedRequestBelowMaxAttemptsThroughProcessor() {
        AccountDeletionRequest failed = AccountDeletionRequest
                .processing(USER_ID, null, NOW.minusSeconds(3600))
                .fail(NOW.minusSeconds(600));
        given(deletionRepositoryPort.findProcessingBatch(20)).willReturn(List.of());
        given(deletionRepositoryPort.findFailedBatch(20, 5, NOW.minusSeconds(300)))
                .willReturn(List.of(failed));

        worker.processPendingRequests();

        verify(processor).process(failed);
        verify(deletionRepositoryPort, never()).save(any());
    }

    @Test
    void keepsFailedRequestWhenMaxAttemptsReachedByNotFetchingItAgain() {
        AccountDeletionRequest exhausted = AccountDeletionRequest
                .processing(USER_ID, null, NOW.minusSeconds(86_400))
                .fail(NOW.minusSeconds(3_600))
                .fail(NOW.minusSeconds(1_800))
                .fail(NOW.minusSeconds(900))
                .fail(NOW.minusSeconds(450))
                .fail(NOW.minusSeconds(60));
        assertThat(exhausted.attemptCount()).isEqualTo(5);
        given(deletionRepositoryPort.findFailedBatch(20, 5, NOW.minusSeconds(300)))
                .willReturn(List.of());

        worker.processPendingRequests();

        verify(processor, never()).process(any());
        verify(deletionRepositoryPort, never()).save(any());
    }

    @Test
    void marksFailureWithIncrementedAttemptCountWhenReprocessingFails() {
        AccountDeletionRequest failed = AccountDeletionRequest
                .processing(USER_ID, null, NOW.minusSeconds(3600))
                .fail(NOW.minusSeconds(600));
        given(deletionRepositoryPort.findProcessingBatch(20)).willReturn(List.of());
        given(deletionRepositoryPort.findFailedBatch(
                ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.any()))
                .willReturn(List.of(failed));
        doThrow(new IllegalStateException("storage unavailable"))
                .when(processor).process(failed);
        given(deletionRepositoryPort.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        worker.processPendingRequests();

        verify(deletionRepositoryPort).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.status() == AccountDeletionStatus.FAILED
                        && saved.attemptCount() == failed.attemptCount() + 1));
    }

    @Test
    void stillProcessesPendingBatchFirst() {
        AccountDeletionRequest processing = AccountDeletionRequest
                .processing(USER_ID, null, NOW.minusSeconds(60));
        given(deletionRepositoryPort.findProcessingBatch(20)).willReturn(List.of(processing));
        given(deletionRepositoryPort.findFailedBatch(
                ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.any()))
                .willReturn(List.of());

        worker.processPendingRequests();

        verify(processor).process(processing);
    }
}
