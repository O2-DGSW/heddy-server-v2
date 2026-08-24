package com.heddy.adapter.in.web.account;

import com.heddy.global.filter.RequestIdFilter;
import com.heddy.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
class ConsentApiIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString(
            "80000000-0000-4000-8000-000000000010");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "80000000-0000-4000-8000-000000000011");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    @BeforeEach
    void setUpUsers() {
        insertUser(USER_ID, "consent-api-user@example.com");
        insertUser(OTHER_USER_ID, "other-consent-api-user@example.com");
    }

    @Test
    void getsLatestHistoryAndDefaultsForAllConsentTypes() throws Exception {
        insertConsent(USER_ID, "AI_TRAINING", false, "2026-08-01", "SIGNUP",
                "2026-08-23T00:00:00Z");
        insertConsent(USER_ID, "AI_TRAINING", true, "2026-08-23", "SETTINGS",
                "2026-08-23T00:00:00Z");
        insertConsent(USER_ID, "PRIVACY_POLICY", true, "2026-08-01", "SIGNUP",
                "2026-08-20T00:00:00Z");
        insertConsent(OTHER_USER_ID, "AI_TRAINING", false, "other-policy", "SETTINGS",
                "2026-08-24T00:00:00Z");

        mockMvc.perform(get("/me/consents")
                        .with(authentication(userAuthentication(USER_ID)))
                        .header(RequestIdFilter.HEADER, "request-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(6))
                .andExpect(jsonPath("$.data.items[0].consent_type")
                        .value("TERMS_OF_SERVICE"))
                .andExpect(jsonPath("$.data.items[0].granted").value(false))
                .andExpect(jsonPath("$.data.items[0].policy_version").isEmpty())
                .andExpect(jsonPath("$.data.items[0].changed_at").isEmpty())
                .andExpect(jsonPath("$.data.items[1].consent_type")
                        .value("PRIVACY_POLICY"))
                .andExpect(jsonPath("$.data.items[2].consent_type")
                        .value("AI_TRAINING"))
                .andExpect(jsonPath("$.data.items[2].granted").value(true))
                .andExpect(jsonPath("$.data.items[2].policy_version")
                        .value("2026-08-23"))
                .andExpect(jsonPath("$.data.items[2].source").doesNotExist())
                .andExpect(jsonPath("$.request_id").value("request-22"));
    }

    @Test
    void appendsConsentChangeWithoutOverwritingExistingHistory() throws Exception {
        insertConsent(USER_ID, "AI_TRAINING", false, "2026-08-01", "SIGNUP",
                "2026-08-20T00:00:00Z");

        mockMvc.perform(put("/me/consents/AI_TRAINING")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeBody(true, "2026-08-01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.consent_type").value("AI_TRAINING"))
                .andExpect(jsonPath("$.data.granted").value(true))
                .andExpect(jsonPath("$.data.policy_version").value("2026-08-01"))
                .andExpect(jsonPath("$.data.source").doesNotExist())
                .andExpect(jsonPath("$.data.changed_at").isString());

        entityManager.flush();
        List<ConsentRow> rows = jdbcTemplate.query("""
                SELECT granted, policy_version, source
                FROM consent_history
                WHERE user_id = ? AND consent_type = 'AI_TRAINING'
                ORDER BY change_sequence
                """, (resultSet, rowNumber) -> new ConsentRow(
                resultSet.getBoolean("granted"),
                resultSet.getString("policy_version"),
                resultSet.getString("source")), USER_ID);
        assertThat(rows).containsExactly(
                new ConsentRow(false, "2026-08-01", "SIGNUP"),
                new ConsentRow(true, "2026-08-01", "SETTINGS"));
    }

    @Test
    void managesAiTrainingAndServiceAnalyticsIndependently() throws Exception {
        insertConsent(USER_ID, "AI_TRAINING", false, "2026-08-01", "SIGNUP",
                "2026-08-20T00:00:00Z");
        insertConsent(USER_ID, "SERVICE_ANALYTICS", true, "2026-08-01", "SIGNUP",
                "2026-08-20T00:00:00Z");

        mockMvc.perform(put("/me/consents/AI_TRAINING")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeBody(true, "2026-08-01")))
                .andExpect(status().isOk());

        Boolean analyticsGranted = jdbcTemplate.queryForObject("""
                SELECT granted FROM consent_history
                WHERE user_id = ? AND consent_type = 'SERVICE_ANALYTICS'
                ORDER BY change_sequence DESC LIMIT 1
                """, Boolean.class, USER_ID);
        Integer analyticsRows = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM consent_history
                WHERE user_id = ? AND consent_type = 'SERVICE_ANALYTICS'
                """, Integer.class, USER_ID);
        assertThat(analyticsGranted).isTrue();
        assertThat(analyticsRows).isEqualTo(1);
    }

    @Test
    void rejectsRequiredConsentWithdrawalAndLeavesHistoryUnchanged() throws Exception {
        insertConsent(USER_ID, "TERMS_OF_SERVICE", true, "2026-08-01", "SIGNUP",
                "2026-08-20T00:00:00Z");

        mockMvc.perform(put("/me/consents/TERMS_OF_SERVICE")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeBody(false, "2026-08-01")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code")
                        .value("CONSENT_WITHDRAWAL_REQUIRES_ACCOUNT_DELETION"))
                .andExpect(jsonPath("$.error.message", containsString("회원 탈퇴")));

        assertConsentCount("TERMS_OF_SERVICE", 1);
    }

    @Test
    void requiresPolicyVersion() throws Exception {
        mockMvc.perform(put("/me/consents/PUSH_NOTIFICATION")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"granted\":true}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.field_errors[0].field")
                        .value("policy_version"));

        assertConsentCount("PUSH_NOTIFICATION", 0);
    }

    @Test
    void rejectsPolicyVersionThatDiffersFromServerConfiguration() throws Exception {
        mockMvc.perform(put("/me/consents/PUSH_NOTIFICATION")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeBody(true, "2026-07-01")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code")
                        .value("CONSENT_POLICY_VERSION_INVALID"));

        assertConsentCount("PUSH_NOTIFICATION", 0);
    }

    @Test
    void rejectsUnknownConsentType() throws Exception {
        mockMvc.perform(put("/me/consents/UNKNOWN")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeBody(true, "2026-08-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/me/consents"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/me/consents/MARKETING_NOTIFICATION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeBody(true, "2026-08-01")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDeletedAccount() throws Exception {
        jdbcTemplate.update(
                "UPDATE users SET status = 'DELETED' WHERE user_id = ?", USER_ID);

        mockMvc.perform(get("/me/consents")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AUTH_ACCOUNT_DELETED"));
    }

    @Test
    void documentsBearerAuthenticationPolicyVersionAndWithdrawalFlow() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/me/consents']['get']"
                        + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$['paths']['/me/consents/{consentType}']['put']"
                        + ".description", containsString("회원 탈퇴")))
                .andExpect(jsonPath("$.components.schemas.ChangeConsentRequest.properties"
                        + ".policy_version.description", containsString("정책 버전")));
    }

    @Test
    void preventsPhysicalUserDeletionWhileConsentEvidenceExists() {
        insertConsent(USER_ID, "AI_TRAINING", true, "2026-08-01", "SIGNUP",
                "2026-08-20T00:00:00Z");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM users WHERE user_id = ?", USER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private String changeBody(boolean granted, String policyVersion) {
        return """
                {
                  "granted":%s,
                  "policy_version":"%s"
                }
                """.formatted(granted, policyVersion);
    }

    private UsernamePasswordAuthenticationToken userAuthentication(UUID userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private void insertUser(UUID userId, String email) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, ?, 'EMAIL', 'ACTIVE', 0)
                """, userId, email, "hash");
    }

    private void insertConsent(
            UUID userId,
            String type,
            boolean granted,
            String policyVersion,
            String source,
            String changedAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO consent_history (
                    consent_id, user_id, consent_type, granted,
                    policy_version, source, changed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), userId, type, granted,
                policyVersion, source, Timestamp.from(Instant.parse(changedAt)));
    }

    private void assertConsentCount(String type, int expected) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM consent_history
                WHERE user_id = ? AND consent_type = ?
                """, Integer.class, USER_ID, type);
        assertThat(count).isEqualTo(expected);
    }

    private record ConsentRow(boolean granted, String policyVersion, String source) {
    }
}
