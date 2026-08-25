package com.heddy.application.account.service;

import com.heddy.domain.account.model.AccountDeletionRequest;
import com.heddy.domain.account.port.out.AccountDeletionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class AccountDeletionWorker {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionWorker.class);
    private static final int BATCH_SIZE = 20;

    private final AccountDeletionRepositoryPort deletionRepositoryPort;
    private final AccountDeletionProcessor processor;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration retryBackoff;

    AccountDeletionWorker(
            AccountDeletionRepositoryPort deletionRepositoryPort,
            AccountDeletionProcessor processor,
            Clock clock,
            @Value("${app.account-deletion.max-attempts:5}") int maxAttempts,
            @Value("${app.account-deletion.retry-backoff-seconds:300}") long retryBackoffSeconds) {
        this.deletionRepositoryPort = deletionRepositoryPort;
        this.processor = processor;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.retryBackoff = Duration.ofSeconds(retryBackoffSeconds);
    }

    @Scheduled(
            fixedDelayString = "${app.account-deletion.worker-delay-ms:5000}",
            initialDelayString = "${app.account-deletion.worker-initial-delay-ms:60000}")
    public void processPendingRequests() {
        for (AccountDeletionRequest request : deletionRepositoryPort.findProcessingBatch(BATCH_SIZE)) {
            handle(request);
        }
        for (AccountDeletionRequest request : deletionRepositoryPort.findFailedBatch(
                BATCH_SIZE, maxAttempts, clock.instant().minus(retryBackoff))) {
            handle(request);
        }
    }

    private void handle(AccountDeletionRequest request) {
        try {
            processor.process(request);
        } catch (RuntimeException failure) {
            log.error("계정 탈퇴 비동기 정리에 실패했습니다. deletionRequestId={}, attemptCount={}",
                    request.deletionRequestId(), request.attemptCount() + 1, failure);
            deletionRepositoryPort.save(request.fail(clock.instant()));
        }
    }
}
