package com.heddy.adapter.in.web.sharing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공개 공유 조회의 HTTP 계약. 링크는 생성 API 로 만들어 원문을 받고(토큰은 응답으로만 존재),
 * 인증 없이 조회한다. 사진 URL 은 LocalStack 흐름 없이 계산되는 서명이라 값 자체는 중요하지 않다.
 */
@Transactional
@AutoConfigureMockMvc
class PublicShareApiIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString(
            "84000000-0000-4000-8000-000000000001");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    private String rawToken;
    private UUID lastRecordId;

    /**
     * Presigned GET 은 네트워크 없이 계산된다. 기본 자격증명 사슬 대신 고정값을 주입해
     * 환경과 무관하게 서명이 만들어지게 한다. 값 자체는 중요하지 않다.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class OfflineSigningConfig {

        @Bean
        @Primary
        AwsCredentialsProvider offlineAwsCredentialsProvider() {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test-access-key", "test-secret-key"));
        }
    }

    @BeforeEach
    void setUpUser() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, ?, 'EMAIL', 'ACTIVE', 0)
                """, USER_ID, USER_ID + "@example.com", "hash");
        jdbcTemplate.update("""
                INSERT INTO user_profiles (user_id, nickname)
                VALUES (?, ?)
                """, USER_ID, "gangmin");
    }

    @Test
    void servesSharedContentWithoutAuthenticationAndForbidsCaching() throws Exception {
        createShareWithFields("[\"PHOTOS\",\"TREATMENT_DETAILS\",\"SATISFACTION\",\"MEMO\"]");
        UUID photoId = insertPhoto(true);

        mockMvc.perform(get("/public/shares/" + rawToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"))
                .andExpect(jsonPath("$.data.share.owner_display_name").value("gangmin"))
                .andExpect(jsonPath("$.data.share.expires_at").isNotEmpty())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].salon_name").value("준헤어"))
                .andExpect(jsonPath("$.data.records[0].service_types[0]").value("CUT"))
                .andExpect(jsonPath("$.data.records[0].satisfaction").value(4))
                .andExpect(jsonPath("$.data.records[0].memo").value("메모"))
                .andExpect(jsonPath("$.data.records[0].photos", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].photos[0].image_type").value("AFTER"))
                .andExpect(jsonPath("$.data.records[0].photos[0].display_url",
                        containsString("X-Amz-Signature")))
                .andExpect(jsonPath("$.data.saved_styles").doesNotExist());
    }

    @Test
    void omitsUnselectedFieldKeysEntirely() throws Exception {
        // PHOTOS 만 선택했다. 시술 정보·만족도·메모는 키 자체가 없어야 한다.
        createShareWithFields("[\"PHOTOS\"]");
        insertPhoto(true);

        mockMvc.perform(get("/public/shares/" + rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].photos", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].performed_at").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].salon_name").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].designer_name").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].service_types").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].satisfaction").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].memo").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].next_visit_cautions").doesNotExist());
    }

    @Test
    void answersEmptySavedStylesArrayWhenSelected() throws Exception {
        createShareWithFields("[\"SAVED_STYLES\"]");

        // 공유된 기록 자체는 남아 있지만 어떤 항목도 선택하지 않아 키가 하나도 없다.
        mockMvc.perform(get("/public/shares/" + rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.saved_styles", hasSize(0)));
    }

    @Test
    void returnsSelectedSavedStyleWithoutInternalIdentifiers() throws Exception {
        UUID savedStyleId = insertSavedStyle();
        createShareWithFields("[\"SAVED_STYLES\"]", savedStyleId);

        mockMvc.perform(get("/public/shares/" + rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved_styles", hasSize(1)))
                .andExpect(jsonPath("$.data.saved_styles[0].style_name")
                        .value("레이어드 커트"))
                .andExpect(jsonPath("$.data.saved_styles[0].image_url")
                        .value("https://images.example.com/layered.jpg"))
                .andExpect(jsonPath("$.data.saved_styles[0].reason")
                        .value("이전 펌 이력 기반 추천"))
                .andExpect(jsonPath("$.data.saved_styles[0].saved_style_id").doesNotExist())
                .andExpect(jsonPath("$.data.saved_styles[0].user_id").doesNotExist());
    }

    @Test
    void hidesNotReadyPhotosFromThePublicResponse() throws Exception {
        createShareWithFields("[\"PHOTOS\"]");
        insertPhoto(false);

        mockMvc.perform(get("/public/shares/" + rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].photos", hasSize(0)));
    }

    @Test
    void deniesUnknownOrTamperedTokensAsNotFound() throws Exception {
        mockMvc.perform(get("/public/shares/not-a-real-token"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(jsonPath("$.error.code").value("SHARE_TOKEN_INVALID"));
    }

    @Test
    void refusesExpiredLinks() throws Exception {
        String token = "expired-token";
        insertShareRow(token, "ACTIVE", Instant.now().minusSeconds(60));

        mockMvc.perform(get("/public/shares/" + token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"))
                .andExpect(jsonPath("$.error.code").value("SHARE_EXPIRED"));
    }

    @Test
    void refusesRevokedLinksImmediatelyAfterWithdrawal() throws Exception {
        createShareWithFields("[\"PHOTOS\"]");
        String shareId = jdbcTemplate.queryForObject(
                "SELECT share_id FROM shares WHERE token_hash = ?", String.class, sha256(rawToken));

        mockMvc.perform(deleteShare(shareId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/public/shares/" + rawToken))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("SHARE_REVOKED"));
    }

    // ------------------------------------------------------------------ 헬퍼

    private org.springframework.test.web.servlet.RequestBuilder deleteShare(String shareId) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/shares/" + shareId)
                .with(authentication(new UsernamePasswordAuthenticationToken(
                        USER_ID, null, List.of())));
    }

    /** 공유를 API 로 만들어 원문 토큰을 필드에 보관한다. 토큰은 이 응답으로만 존재한다. */
    private void createShareWithFields(String fieldsJson) throws Exception {
        createShareWithFields(fieldsJson, null);
    }

    private void createShareWithFields(String fieldsJson, UUID savedStyleId) throws Exception {
        UUID recordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_records (
                    record_id, user_id, service_types, salon_name, designer_name,
                    performed_at, satisfaction, memo
                ) VALUES (?, ?, ?::jsonb, '준헤어', '김실장', now(), 4, '메모')
                """, recordId, USER_ID, "[\"CUT\"]");

        String created = mockMvc.perform(post("/shares")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                USER_ID, null, List.of())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"record_ids\":[\"" + recordId + "\"],"
                                + (savedStyleId == null ? ""
                                        : "\"saved_style_ids\":[\"" + savedStyleId + "\"],")
                                + "\"fields\":" + fieldsJson + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        rawToken = new ObjectMapper().readTree(created).path("data").path("share_url")
                .asText().substring("https://heddy.example.com/s/".length());
        lastRecordId = recordId;
    }

    private UUID insertSavedStyle() {
        UUID savedStyleId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO saved_styles (
                    saved_style_id, user_id, style_name, image_url, reason
                ) VALUES (?, ?, '레이어드 커트',
                          'https://images.example.com/layered.jpg',
                          '이전 펌 이력 기반 추천')
                """, savedStyleId, USER_ID);
        return savedStyleId;
    }

    /** 사진 파일과 연결을 심는다. ready=false 면 PENDING 이라 공개 응답에서 빠진다. */
    private UUID insertPhoto(boolean ready) {
        UUID fileId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO files (
                    file_id, upload_id, user_id, purpose, status, object_key,
                    content_type, file_name, file_size, expires_at
                ) VALUES (?, ?, ?, 'TREATMENT_PHOTO', ?, ?, 'image/jpeg', 'after.jpg',
                          1024, now() + interval '5 minutes')
                """, fileId, UUID.randomUUID(), USER_ID, ready ? "READY" : "PENDING",
                "TREATMENT_PHOTO/x/" + fileId);
        jdbcTemplate.update("""
                INSERT INTO treatment_record_photos (
                    photo_id, record_id, file_id, image_type, sort_order, created_at
                ) VALUES (?, ?, ?, 'AFTER', 0, now())
                """, UUID.randomUUID(), lastRecordId, fileId);
        return fileId;
    }

    private void insertShareRow(String token, String status, Instant expiresAt) {
        // 조인 행이 하나라 있어야 도메인 재구성(선택 불변식)을 통과한다.
        UUID recordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_records (
                    record_id, user_id, service_types, performed_at
                ) VALUES (?, ?, ?::jsonb, now())
                """, recordId, USER_ID, "[\"CUT\"]");
        UUID shareId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO shares (
                    share_id, user_id, token_hash, status, expires_at
                ) VALUES (?, ?, ?, ?, ?)
                """, shareId, USER_ID, sha256Quiet(token), status,
                Timestamp.from(expiresAt));
        jdbcTemplate.update("""
                INSERT INTO share_records (share_id, record_id) VALUES (?, ?)
                """, shareId, recordId);
        // 항목 행도 필요하다 — 없으면 재구성 때 선택 불변식 위반으로 읽히지 않는다.
        jdbcTemplate.update("""
                INSERT INTO share_fields (share_id, field_type) VALUES (?, 'PHOTOS')
                """, shareId);
    }

    private String sha256(String value) {
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private String sha256Quiet(String value) {
        return sha256(value);
    }
}
