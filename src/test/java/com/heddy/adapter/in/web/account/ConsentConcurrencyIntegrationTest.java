package com.heddy.adapter.in.web.account;

import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@AutoConfigureMockMvc
class ConsentConcurrencyIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString(
            "80000000-0000-4000-8000-000000000020");
    private static final int REQUEST_COUNT = 8;

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpUser() {
        deleteUser();
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, ?, 'EMAIL', 'ACTIVE', 0)
                """, USER_ID, "concurrent-consent-user@example.com", "hash");
    }

    @AfterEach
    void tearDownUser() {
        deleteUser();
    }

    @Test
    void serializesConcurrentChangesIntoStrictlyOrderedAppendOnlyHistory() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);
        CountDownLatch ready = new CountDownLatch(REQUEST_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> responses = new ArrayList<>();
            for (int index = 0; index < REQUEST_COUNT; index++) {
                boolean granted = index % 2 == 0;
                responses.add(executor.submit(
                        () -> performPutWhenStarted(granted, ready, start)));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            for (Future<Integer> response : responses) {
                assertThat(response.get(15, TimeUnit.SECONDS)).isEqualTo(200);
            }
        } finally {
            executor.shutdownNow();
        }

        Integer historyCount = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM consent_history
                WHERE user_id = ? AND consent_type = 'AI_TRAINING'
                """, Integer.class, USER_ID);
        Integer distinctChangedAtCount = jdbcTemplate.queryForObject("""
                SELECT count(DISTINCT changed_at) FROM consent_history
                WHERE user_id = ? AND consent_type = 'AI_TRAINING'
                """, Integer.class, USER_ID);
        assertThat(historyCount).isEqualTo(REQUEST_COUNT);
        assertThat(distinctChangedAtCount).isEqualTo(REQUEST_COUNT);
    }

    private int performPutWhenStarted(
            boolean granted,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent requests did not start in time");
        }
        return mockMvc.perform(put("/me/consents/AI_TRAINING")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                USER_ID, null, List.of())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "granted":%s,
                                  "policy_version":"2026-08-23"
                                }
                                """.formatted(granted)))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private void deleteUser() {
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", USER_ID);
    }
}
