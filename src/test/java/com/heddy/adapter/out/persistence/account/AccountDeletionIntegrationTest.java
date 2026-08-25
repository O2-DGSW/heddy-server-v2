package com.heddy.adapter.out.persistence.account;

import com.heddy.application.account.service.AccountDeletionProcessor;
import com.heddy.application.account.service.AccountDeletionService;
import com.heddy.domain.account.model.AccountDeletionRequest;
import com.heddy.domain.account.port.in.RequestAccountDeletionUseCase;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class AccountDeletionIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString(
            "83000000-0000-4000-8000-000000000001");

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired AccountDeletionService deletionService;
    @Autowired AccountDeletionProcessor deletionProcessor;
    @Autowired AuthTokenPort authTokenPort;

    private UUID fileId;
    private UUID recordId;

    @BeforeEach
    void setUpData() {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, 'delete-me@example.com', 'hash', 'EMAIL', 'ACTIVE', 0)
                """, USER_ID);
        jdbcTemplate.update("""
                INSERT INTO user_profiles (user_id, nickname, phone)
                VALUES (?, '탈퇴사용자', '01099998888')
                """, USER_ID);
        jdbcTemplate.update("""
                INSERT INTO consent_history (
                    consent_id, user_id, consent_type, granted,
                    policy_version, source, changed_at
                ) VALUES (?, ?, 'PRIVACY_POLICY', true, '2026-08-01', 'SIGNUP', now())
                """, UUID.randomUUID(), USER_ID);
        jdbcTemplate.update("""
                INSERT INTO refresh_tokens (
                    refresh_token_id, user_id, token_hash, expires_at
                ) VALUES (?, ?, ?, now() + interval '1 day')
                """, UUID.randomUUID(), USER_ID, "a".repeat(64));

        fileId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO files (
                    file_id, upload_id, user_id, purpose, status, object_key,
                    content_type, file_name, file_size, expires_at
                ) VALUES (?, ?, ?, 'TREATMENT_PHOTO', 'READY', ?,
                          'image/jpeg', 'photo.jpg', 100, now() + interval '1 hour')
                """, fileId, UUID.randomUUID(), USER_ID, "TREATMENT_PHOTO/" + fileId);
        recordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_records (
                    record_id, user_id, service_types, performed_at
                ) VALUES (?, ?, '["CUT"]'::jsonb, ?)
                """, recordId, USER_ID, Timestamp.from(Instant.parse("2026-08-01T00:00:00Z")));
        jdbcTemplate.update("""
                INSERT INTO treatment_record_photos (
                    photo_id, record_id, file_id, image_type, sort_order
                ) VALUES (?, ?, ?, 'AFTER', 0)
                """, UUID.randomUUID(), recordId, fileId);
    }

    @Test
    void blocksAccountImmediatelyThenAnonymizesDataWhilePreservingConsentEvidence() {
        String token = authTokenPort.createReauthenticationToken(USER_ID);

        AccountDeletionRequest request = deletionService.request(
                new RequestAccountDeletionUseCase.Command(USER_ID, token, "사용 중단"));

        assertThat(request.status().name()).isEqualTo("PROCESSING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM users WHERE user_id = ?", String.class, USER_ID))
                .isEqualTo("DELETION_PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
                Integer.class, USER_ID)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM used_reauthentication_tokens WHERE user_id = ?",
                Integer.class, USER_ID)).isEqualTo(1);

        deletionProcessor.process(request);

        var deletedAccount = jdbcTemplate.queryForMap(
                "SELECT email, password_hash, provider_subject, status FROM users WHERE user_id = ?",
                USER_ID);
        assertThat(deletedAccount.get("email")).isNull();
        assertThat(deletedAccount.get("password_hash")).isNull();
        assertThat(deletedAccount.get("provider_subject")).isNull();
        assertThat(deletedAccount.get("status")).isEqualTo("DELETED");
        assertThat(count("user_profiles", "user_id")).isZero();
        assertThat(count("treatment_records", "user_id")).isZero();
        assertThat(count("consent_history", "user_id")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM files WHERE file_id = ?", String.class, fileId))
                .isEqualTo("DELETED");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT status FROM account_deletion_requests WHERE deletion_request_id = ?
                """, String.class, request.deletionRequestId())).isEqualTo("COMPLETED");
    }

    private int count(String table, String userColumn) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + userColumn + " = ?",
                Integer.class, USER_ID);
        return count == null ? 0 : count;
    }
}
