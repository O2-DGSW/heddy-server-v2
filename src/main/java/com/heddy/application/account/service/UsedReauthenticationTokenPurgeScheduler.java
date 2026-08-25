package com.heddy.application.account.service;

import com.heddy.domain.account.port.out.AccountDeletionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * 사용 완료 재인증 토큰(used_reauthentication_tokens)의 보존 기간이 지난 행을 주기적으로
 * 삭제하는 배치. 삭제 조건은 used_at 인덱스(idx_used_reauthentication_tokens_used_at)로
 * 처리된다.
 */
@Component
public class UsedReauthenticationTokenPurgeScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(UsedReauthenticationTokenPurgeScheduler.class);

    private final AccountDeletionRepositoryPort deletionRepositoryPort;
    private final Clock clock;
    private final Duration retention;

    UsedReauthenticationTokenPurgeScheduler(
            AccountDeletionRepositoryPort deletionRepositoryPort,
            Clock clock,
            @Value("${app.account-deletion.used-token-retention-days:30}") long retentionDays) {
        this.deletionRepositoryPort = deletionRepositoryPort;
        this.clock = clock;
        this.retention = Duration.ofDays(retentionDays);
    }

    @Scheduled(
            fixedDelayString = "${app.account-deletion.token-purge-interval-ms:3600000}",
            initialDelayString = "${app.account-deletion.token-purge-initial-delay-ms:60000}")
    @Transactional
    public int purgeExpiredTokens() {
        Instant threshold = clock.instant().minus(retention);
        int deleted = deletionRepositoryPort.deleteUsedTokensBefore(threshold);
        if (deleted > 0) {
            log.info("만료된 사용 완료 재인증 토큰을 삭제했습니다. deletedCount={}, threshold={}",
                    deleted, threshold);
        }
        return deleted;
    }
}
