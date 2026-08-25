package com.heddy.application.account.service;

import com.heddy.domain.account.model.AccountDeletionRequest;
import com.heddy.domain.account.port.out.AccountDeletionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AccountDeletionWorker {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionWorker.class);
    private static final int BATCH_SIZE = 20;

    private final AccountDeletionRepositoryPort deletionRepositoryPort;
    private final AccountDeletionProcessor processor;

    @Scheduled(
            fixedDelayString = "${app.account-deletion.worker-delay-ms:5000}",
            initialDelayString = "${app.account-deletion.worker-initial-delay-ms:60000}")
    public void processPendingRequests() {
        for (AccountDeletionRequest request : deletionRepositoryPort.findProcessingBatch(BATCH_SIZE)) {
            try {
                processor.process(request);
            } catch (RuntimeException failure) {
                log.error("계정 탈퇴 비동기 정리에 실패했습니다. deletionRequestId={}",
                        request.deletionRequestId(), failure);
                deletionRepositoryPort.save(request.fail(Instant.now()));
            }
        }
    }
}
