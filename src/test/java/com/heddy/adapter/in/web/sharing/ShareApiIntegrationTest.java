package com.heddy.adapter.in.web.sharing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공유 생성·목록의 HTTP 계약을 Postgres 를 상대로 본다. 시술기록은 업로드 흐름을 밟지 않고
 * treatment_records 행을 직접 심는다 — 이 테스트가 검증할 것은 소유권 은닉과 토큰 해시 저장이지
 * 기록 등록 프로토콜이 아니다.
 */
@Transactional
@AutoConfigureMockMvc
class ShareApiIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString(
            "83000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "83000000-0000-4000-8000-000000000002");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpUsers() {
        insertUser(USER_ID);
        insertUser(OTHER_USER_ID);
    }

    // ------------------------------------------------------------------ 생성

    @Test
    void createsShareAndReturnsTheUrlOnlyOnce() throws Exception {
        UUID recordId = insertRecord(USER_ID);

        String created = mockMvc.perform(post("/shares")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"record_ids":["%s"],
                                 "fields":["PHOTOS","TREATMENT_DETAILS"],
                                 "expires_in_days":3}
                                """.formatted(recordId))
                        .header("X-Request-Id", "request-49"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.share_id").isNotEmpty())
                .andExpect(jsonPath("$.data.share_url", containsString("/s/")))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.fields",
                        containsInAnyOrder("PHOTOS", "TREATMENT_DETAILS")))
                .andExpect(jsonPath("$.data.expires_at").isNotEmpty())
                .andExpect(jsonPath("$.request_id").value("request-49"))
                .andReturn().getResponse().getContentAsString();

        String shareUrl = new ObjectMapper().readTree(created)
                .path("data").path("share_url").asText();
        String rawToken = shareUrl.substring(shareUrl.lastIndexOf('/') + 1);

        Integer joinCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM share_records WHERE record_id = ?",
                Integer.class, recordId);
        assertThat(joinCount).isEqualTo(1);
        Integer fieldCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM share_fields WHERE share_id = ("
                        + "SELECT share_id FROM shares WHERE token_hash = ?)",
                Integer.class, sha256(rawToken));
        assertThat(fieldCount).isEqualTo(2);
        // DB 에는 해시만 있고 원문은 없다.
        Integer rawRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM shares WHERE token_hash = ?", Integer.class, rawToken);
        assertThat(rawRows).isZero();
    }

    @Test
    void defaultsExpiryToSevenDaysWhenOmitted() throws Exception {
        UUID recordId = insertRecord(USER_ID);

        String created = mockMvc.perform(post("/shares")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(recordId, null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String expiresAt = new ObjectMapper().readTree(created)
                .path("data").path("expires_at").asText();

        Instant expectedFloor = Instant.now().plusSeconds(7 * 86_400 - 60);
        assertThat(Instant.parse(expiresAt)).isAfter(expectedFloor);
    }

    @Test
    void rejectsSelectionWithoutTargetsOrFieldsWithTheSpecErrorCode() throws Exception {
        mockMvc.perform(post("/shares")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"record_ids\":[],\"fields\":[\"PHOTOS\"]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("SHARE_EMPTY_SELECTION"));

        UUID recordId = insertRecord(USER_ID);
        mockMvc.perform(post("/shares")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"record_ids\":[\"" + recordId + "\"],\"fields\":[]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("SHARE_EMPTY_SELECTION"));
    }

    @Test
    void rejectsUnknownFieldTypeAsBadRequest() throws Exception {
        mockMvc.perform(post("/shares")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"record_ids\":[],\"fields\":[\"NOT_A_FIELD\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void hidesForeignRecordBehindResourceNotFound() throws Exception {
        UUID foreignRecordId = insertRecord(OTHER_USER_ID);

        mockMvc.perform(post("/shares")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(foreignRecordId, 3)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM shares", Integer.class);
        assertThat(count).isZero();
    }

    @Test
    void hidesUnknownRecordBehindResourceNotFound() throws Exception {
        mockMvc.perform(post("/shares")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(UUID.randomUUID(), null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    // ------------------------------------------------------------------ 목록

    @Test
    void listsOwnSharesNewestFirstWithOptionalStatusFilter() throws Exception {
        UUID recordId = insertRecord(USER_ID);
        createShare(recordId);
        String second = createShare(recordId);
        String secondId = new ObjectMapper().readTree(second)
                .path("data").path("share_id").asText();

        String listed = mockMvc.perform(get("/shares")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.page.total_elements").value(2))
                .andExpect(jsonPath("$.data.page.has_next").value(false))
                .andExpect(jsonPath("$.data.items[0].share_url").doesNotExist())
                .andExpect(jsonPath("$.request_id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(new ObjectMapper().readTree(listed).path("data").path("items")
                .get(0).path("share_id").asText()).isEqualTo(secondId);

        // 아직 철회 API(#50)가 없어 철회 상태를 SQL 로 심어 필터를 검증한다.
        jdbcTemplate.update(
                "UPDATE shares SET status = 'REVOKED', revoked_at = now() "
                        + "WHERE share_id <> ?",
                UUID.fromString(secondId));

        mockMvc.perform(get("/shares")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].share_id").value(secondId));

        mockMvc.perform(get("/shares")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("status", "REVOKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    void answersAnEmptyListInsteadOfNotFound() throws Exception {
        mockMvc.perform(get("/shares")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.page.total_elements").value(0));
    }

    @Test
    void doesNotLeakAnotherUsersSharesInTheList() throws Exception {
        UUID recordId = insertRecord(USER_ID);
        createShare(recordId);

        mockMvc.perform(get("/shares")
                        .with(authentication(userAuthentication(OTHER_USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    @Test
    void rejectsInvalidPagingAndUnknownStatusAsBadRequest() throws Exception {
        mockMvc.perform(get("/shares")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/shares")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/shares"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ 상세·수정·철회

    @Test
    void readsBackOwnedShareDetailWithTargets() throws Exception {
        UUID recordId = insertRecord(USER_ID);
        String created = createShare(recordId);
        String shareId = shareIdOf(created);

        mockMvc.perform(get("/shares/" + shareId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .header("X-Request-Id", "request-50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.share_id").value(shareId))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.fields", containsInAnyOrder("PHOTOS")))
                .andExpect(jsonPath("$.data.record_ids", hasSize(1)))
                .andExpect(jsonPath("$.data.saved_style_ids", hasSize(0)))
                .andExpect(jsonPath("$.data.revoked_at").isEmpty())
                .andExpect(jsonPath("$.request_id").value("request-50"));
    }

    @Test
    void patchesOnlyPresentedFieldsAndKeepsTargets() throws Exception {
        UUID recordId = insertRecord(USER_ID);
        String created = createShare(recordId);
        String shareId = shareIdOf(created);

        mockMvc.perform(patch("/shares/" + shareId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fields\":[\"MEMO\",\"CAUTIONS\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fields",
                        containsInAnyOrder("MEMO", "CAUTIONS")))
                .andExpect(jsonPath("$.data.expires_at").isNotEmpty());

        mockMvc.perform(patch("/shares/" + shareId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expires_at\":\"2027-01-01T00:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expires_at").value("2027-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.fields",
                        containsInAnyOrder("MEMO", "CAUTIONS")))
                .andExpect(jsonPath("$.data.record_ids", hasSize(1)));
    }

    @Test
    void rejectsPatchWithEmptyFieldsOrPastExpiryAs422() throws Exception {
        UUID recordId = insertRecord(USER_ID);
        String shareId = shareIdOf(createShare(recordId));

        mockMvc.perform(patch("/shares/" + shareId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fields\":[]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("SHARE_EMPTY_SELECTION"));

        mockMvc.perform(patch("/shares/" + shareId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expires_at\":\"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code")
                        .value("SHARING_EXPIRES_AT_NOT_FUTURE"));
    }

    @Test
    void revokesImmediatelyAndKeepsTheRow() throws Exception {
        UUID recordId = insertRecord(USER_ID);
        String shareId = shareIdOf(createShare(recordId));

        mockMvc.perform(delete("/shares/" + shareId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM shares WHERE share_id = ?",
                String.class, UUID.fromString(shareId))).isEqualTo("REVOKED");
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM shares WHERE share_id = ? AND revoked_at IS NOT NULL",
                Integer.class, UUID.fromString(shareId));
        assertThat(rows).isEqualTo(1);

        // 철회는 멱등이다.
        mockMvc.perform(delete("/shares/" + shareId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNoContent());
    }

    @Test
    void hidesForeignShareDuringGetPatchAndDelete() throws Exception {
        UUID recordId = insertRecord(USER_ID);
        String shareId = shareIdOf(createShare(recordId));

        mockMvc.perform(get("/shares/" + shareId)
                        .with(authentication(userAuthentication(OTHER_USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(patch("/shares/" + shareId)
                        .with(authentication(userAuthentication(OTHER_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fields\":[\"MEMO\"]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(delete("/shares/" + shareId)
                        .with(authentication(userAuthentication(OTHER_USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        // 남의 요청으로 행이 바뀌지 않았다.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM shares WHERE share_id = ?",
                String.class, UUID.fromString(shareId))).isEqualTo("ACTIVE");
    }

    @Test
    void answers404ForUnknownShareOnDetailPatchAndDelete() throws Exception {
        String unknown = UUID.randomUUID().toString();

        mockMvc.perform(get("/shares/" + unknown)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/shares/" + unknown)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/shares/" + unknown)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ 헬퍼

    private String createShare(UUID recordId) throws Exception {
        String created = mockMvc.perform(post("/shares")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(recordId, null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return created;
    }

    private String createBody(UUID recordId, Integer expiresInDays) {
        return """
                {"record_ids":["%s"],"fields":["PHOTOS"]%s}
                """.formatted(recordId,
                expiresInDays == null ? "" : ",\"expires_in_days\":" + expiresInDays);
    }

    private String shareIdOf(String createdBody) throws Exception {
        return new ObjectMapper().readTree(createdBody)
                .path("data").path("share_id").asText();
    }

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

    private UUID insertRecord(UUID ownerId) {
        UUID recordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_records (
                    record_id, user_id, service_types, performed_at
                ) VALUES (?, ?, ?::jsonb, now())
                """, recordId, ownerId, "[\"CUT\"]");
        return recordId;
    }

    private String sha256(String value) throws Exception {
        java.security.MessageDigest digest =
                java.security.MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(hashed);
    }
}
