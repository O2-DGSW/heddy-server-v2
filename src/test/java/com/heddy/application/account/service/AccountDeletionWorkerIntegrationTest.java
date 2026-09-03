package com.heddy.application.account.service;

import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class AccountDeletionWorkerIntegrationTest extends PostgresIntegrationTest {

    private static final long RETRY_BACKOFF_SECONDS = 300;
    private static final int MAX_ATTEMPTS = 5;

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired AccountDeletionWorker worker;

    @Test
    void requeuesFailedRequestBelowMaxAttemptsAndCompletesCleanup() {
        UUID userId = seedUser();
        UUID fileId = seedUserAssets(userId);
        UUID requestId = seedDeletionRequest(userId, 1, Instant.now().minusSeconds(600));

        worker.processPendingRequests();

        assertThat(requestStatus(requestId)).isEqualTo("COMPLETED");
        assertThat(userStatus(userId)).isEqualTo("DELETED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_profiles WHERE user_id = ?",
                Integer.class, userId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM treatment_records WHERE user_id = ?",
                Integer.class, userId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM saved_styles WHERE user_id = ?",
                Integer.class, userId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM shares WHERE user_id = ?",
                Integer.class, userId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_style_preferences WHERE user_id = ?",
                Integer.class, userId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM files WHERE file_id = ?", String.class, fileId))
                .isEqualTo("DELETED");
    }

    @Test
    void keepsFailedRequestThatReachedMaxAttempts() {
        UUID userId = seedUser();
        UUID requestId = seedDeletionRequest(
                userId, MAX_ATTEMPTS, Instant.now().minusSeconds(3_600));

        worker.processPendingRequests();

        assertThat(requestStatus(requestId)).isEqualTo("FAILED");
        assertThat(requestAttemptCount(requestId)).isEqualTo(MAX_ATTEMPTS);
    }

    @Test
    void keepsRecentlyFailedRequestInsideRetryBackoffWindow() {
        UUID userId = seedUser();
        UUID requestId = seedDeletionRequest(userId, 1, Instant.now().minusSeconds(60));

        worker.processPendingRequests();

        assertThat(requestStatus(requestId)).isEqualTo("FAILED");
    }

    private UUID seedUser() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, 'retry-me@example.com', 'hash', 'EMAIL', 'ACTIVE', 0)
                """, userId);
        return userId;
    }

    private UUID seedUserAssets(UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO user_profiles (user_id, nickname, phone)
                VALUES (?, '재처리사용자', '01077776666')
                """, userId);
        UUID fileId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO files (
                    file_id, upload_id, user_id, purpose, status, object_key,
                    content_type, file_name, file_size, expires_at
                ) VALUES (?, ?, ?, 'TREATMENT_PHOTO', 'READY', ?,
                          'image/jpeg', 'photo.jpg', 100, now() + interval '1 hour')
                """, fileId, UUID.randomUUID(), userId, "TREATMENT_PHOTO/" + fileId);
        UUID recordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_records (
                    record_id, user_id, service_types, performed_at
                ) VALUES (?, ?, '["CUT"]'::jsonb, ?)
                """, recordId, userId, Timestamp.from(Instant.parse("2026-08-01T00:00:00Z")));
        jdbcTemplate.update("""
                INSERT INTO treatment_record_photos (
                    photo_id, record_id, file_id, image_type, sort_order
                ) VALUES (?, ?, ?, 'AFTER', 0)
                """, UUID.randomUUID(), recordId, fileId);
        UUID savedStyleId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO saved_styles (
                    saved_style_id, user_id, style_name, image_url, reason, memo
                ) VALUES (?, ?, '레이어드 커트', 'https://example.com/style.jpg',
                          '과거 만족도가 높음', '상담 때 보여주기')
                """, savedStyleId, userId);
        UUID shareId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO shares (
                    share_id, user_id, token_hash, status, expires_at
                ) VALUES (?, ?, ?, 'ACTIVE', now() + interval '1 day')
                """, shareId, userId, UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", ""));
        jdbcTemplate.update("""
                INSERT INTO share_records (share_id, record_id) VALUES (?, ?)
                """, shareId, recordId);
        jdbcTemplate.update("""
                INSERT INTO share_saved_styles (share_id, saved_style_id) VALUES (?, ?)
                """, shareId, savedStyleId);
        UUID styleTagId = jdbcTemplate.queryForObject(
                "SELECT style_tag_id FROM style_tags ORDER BY style_tag_id LIMIT 1", UUID.class);
        jdbcTemplate.update("""
                INSERT INTO user_style_preferences (
                    preference_id, user_id, style_tag_id, preference_type
                ) VALUES (?, ?, ?, 'PREFERRED')
                """, UUID.randomUUID(), userId, styleTagId);
        return fileId;
    }

    private UUID seedDeletionRequest(UUID userId, int attemptCount, Instant completedAt) {
        UUID requestId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO account_deletion_requests (
                    deletion_request_id, user_id, status, reason,
                    requested_at, completed_at, attempt_count
                ) VALUES (?, ?, 'FAILED', '사유', now() - interval '2 hours', ?, ?)
                """, requestId, userId, Timestamp.from(completedAt), attemptCount);
        return requestId;
    }

    private String requestStatus(UUID requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM account_deletion_requests WHERE deletion_request_id = ?",
                String.class, requestId);
    }

    private int requestAttemptCount(UUID requestId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT attempt_count FROM account_deletion_requests WHERE deletion_request_id = ?",
                Integer.class, requestId);
        return count == null ? 0 : count;
    }

    private String userStatus(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM users WHERE user_id = ?", String.class, userId);
    }
}
