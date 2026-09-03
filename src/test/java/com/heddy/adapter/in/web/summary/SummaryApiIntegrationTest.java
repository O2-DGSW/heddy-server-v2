package com.heddy.adapter.in.web.summary;

import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 홈 요약 카운트의 HTTP 계약. 화면이 이 값을 그대로 타일에 박으므로 필드 이름과 단위가
 * 계약이다 — shared_record_count 는 공유 링크 수가 아니라 기록 수다.
 */
@Transactional
@AutoConfigureMockMvc
class SummaryApiIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("85000000-0000-4000-8000-000000000001");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpUser() {
        insertUser(USER_ID);
    }

    @Test
    void answersZerosForAnEmptyAccount() throws Exception {
        mockMvc.perform(get("/me/summary")
                        .with(authentication(userAuthentication(USER_ID)))
                        .header("X-Request-Id", "request-161"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.treatment_record_count").value(0))
                .andExpect(jsonPath("$.data.analyzed_record_count").value(0))
                .andExpect(jsonPath("$.data.saved_style_count").value(0))
                .andExpect(jsonPath("$.data.shared_record_count").value(0))
                .andExpect(jsonPath("$.request_id").value("request-161"));
    }

    /** 기록 2건을 링크 3개로 공유해도 "공유 중" 은 2 다. 링크 수를 세면 3 이 나온다. */
    @Test
    void countsSharedRecordsRatherThanShareLinks() throws Exception {
        UUID first = insertRecord();
        UUID second = insertRecord();
        insertRecord();
        insertActiveShare(first);
        insertActiveShare(first);
        insertActiveShare(second);

        mockMvc.perform(get("/me/summary")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.treatment_record_count").value(3))
                .andExpect(jsonPath("$.data.shared_record_count").value(2));
    }

    @Test
    void rejectsAnAnonymousCall() throws Exception {
        mockMvc.perform(get("/me/summary"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ 헬퍼

    private UsernamePasswordAuthenticationToken userAuthentication(UUID userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private void insertUser(UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, ?, 'EMAIL', 'ACTIVE', 0)
                """, userId, userId + "@example.com", "hash");
    }

    private UUID insertRecord() {
        UUID recordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_records (record_id, user_id, service_types, performed_at)
                VALUES (?, ?, ?::jsonb, now())
                """, recordId, USER_ID, "[\"CUT\"]");
        return recordId;
    }

    private void insertActiveShare(UUID recordId) {
        UUID shareId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO shares (share_id, user_id, token_hash, status, expires_at)
                VALUES (?, ?, ?, 'ACTIVE', now() + interval '7 days')
                """, shareId, USER_ID, UUID.randomUUID().toString().replace("-", ""));
        jdbcTemplate.update(
                "INSERT INTO share_records (share_id, record_id) VALUES (?, ?)",
                shareId, recordId);
    }
}
