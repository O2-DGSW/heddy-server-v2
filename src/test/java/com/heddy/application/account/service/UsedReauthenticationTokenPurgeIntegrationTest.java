package com.heddy.application.account.service;

import com.heddy.domain.account.port.out.AccountDeletionRepositoryPort;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class UsedReauthenticationTokenPurgeIntegrationTest extends PostgresIntegrationTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired UsedReauthenticationTokenPurgeScheduler purgeScheduler;
    @Autowired AccountDeletionRepositoryPort deletionRepositoryPort;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, 'purge-me@example.com', 'hash', 'EMAIL', 'ACTIVE', 0)
                """, userId);
    }

    @Test
    void deletesOnlyTokensPastConfiguredRetention() {
        UUID expired = seedToken(NOW.minusSeconds(31 * 86_400L));
        UUID withinRetention = seedToken(NOW.minusSeconds(3_600L));

        int deleted = purgeScheduler.purgeExpiredTokens();

        assertThat(deleted).isEqualTo(1);
        assertThat(tokenCount(expired)).isZero();
        assertThat(tokenCount(withinRetention)).isEqualTo(1);
    }

    @Test
    void keepsTokenAtExactRetentionThresholdAndDeletesOnlyOlderOnes() {
        Instant threshold = Instant.now().truncatedTo(ChronoUnit.SECONDS)
                .minusSeconds(30 * 86_400L);
        UUID atThreshold = seedToken(threshold);
        UUID beyondThreshold = seedToken(threshold.minusNanos(1_000));

        int deleted = deletionRepositoryPort.deleteUsedTokensBefore(threshold);

        assertThat(deleted).isEqualTo(1);
        assertThat(tokenCount(beyondThreshold)).isZero();
        assertThat(tokenCount(atThreshold)).isEqualTo(1);
    }

    private UUID seedToken(Instant usedAt) {
        UUID tokenId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO used_reauthentication_tokens (token_id, user_id, used_at)
                VALUES (?, ?, ?)
                """, tokenId, userId, Timestamp.from(usedAt));
        return tokenId;
    }

    private int tokenCount(UUID tokenId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM used_reauthentication_tokens WHERE token_id = ?",
                Integer.class, tokenId);
        return count == null ? 0 : count;
    }
}
