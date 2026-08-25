package com.heddy.application.account.service;

import com.heddy.domain.account.port.out.AccountDeletionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsedReauthenticationTokenPurgeSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");

    @Mock AccountDeletionRepositoryPort deletionRepositoryPort;

    private UsedReauthenticationTokenPurgeScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new UsedReauthenticationTokenPurgeScheduler(
                deletionRepositoryPort, Clock.fixed(NOW, ZoneOffset.UTC), 30);
    }

    @Test
    void deletesOnlyTokensOlderThanConfiguredRetention() {
        given(deletionRepositoryPort.deleteUsedTokensBefore(NOW.minusSeconds(30 * 86_400L)))
                .willReturn(2);

        int deleted = scheduler.purgeExpiredTokens();

        assertThat(deleted).isEqualTo(2);
        verify(deletionRepositoryPort).deleteUsedTokensBefore(NOW.minusSeconds(30 * 86_400L));
    }
}
