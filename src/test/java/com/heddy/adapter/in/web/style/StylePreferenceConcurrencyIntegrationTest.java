package com.heddy.adapter.in.web.style;

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
class StylePreferenceConcurrencyIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString(
            "70000000-0000-4000-8000-000000000010");
    private static final String REQUEST_BODY = """
            {
              "preferred_tag_ids":["10000000-0000-4000-8000-000000000001"],
              "excluded_tag_ids":["50000000-0000-4000-8000-000000000001"]
            }
            """;

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpUser() {
        deleteUser();
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, ?, 'EMAIL', 'ACTIVE', 0)
                """, USER_ID, "concurrent-style-user@example.com", "hash");
    }

    @AfterEach
    void tearDownUser() {
        deleteUser();
    }

    @Test
    void identicalConcurrentPutRequestsAllSucceedAndLeaveOneReplacement() throws Exception {
        int requestCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> responses = new ArrayList<>();
            for (int index = 0; index < requestCount; index++) {
                responses.add(executor.submit(() -> performPutWhenStarted(ready, start)));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            for (Future<Integer> response : responses) {
                assertThat(response.get(15, TimeUnit.SECONDS)).isEqualTo(200);
            }
        } finally {
            executor.shutdownNow();
        }

        Integer savedCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_style_preferences WHERE user_id = ?",
                Integer.class, USER_ID);
        assertThat(savedCount).isEqualTo(2);
    }

    private int performPutWhenStarted(CountDownLatch ready, CountDownLatch start)
            throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent requests did not start in time");
        }
        return mockMvc.perform(put("/me/style-preferences")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                USER_ID, null, List.of())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private void deleteUser() {
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", USER_ID);
    }
}
