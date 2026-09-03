package com.heddy.adapter.in.web.style;

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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "저장한 후보 스타일" 화면의 HTTP 계약. 캡처 파일은 업로드 프로토콜을 밟지 않고 files 행을
 * 직접 심어 READY·PENDING 을 만든다 — 여기서 볼 것은 카탈로그 참조 규칙과 URL 발급이다.
 */
@Transactional
@AutoConfigureMockMvc
class SavedStyleApiIntegrationTest extends PostgresIntegrationTest {

    /** Presigned GET 은 네트워크 없이 계산된다. 환경과 무관하게 서명이 만들어지도록 고정값을 준다. */
    @TestConfiguration(proxyBeanMethods = false)
    static class OfflineSigningConfig {

        @Bean
        @Primary
        AwsCredentialsProvider offlineAwsCredentialsProvider() {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test-access-key", "test-secret-key"));
        }
    }

    private static final UUID USER_ID = UUID.fromString(
            "88000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "88000000-0000-4000-8000-000000000002");
    private static final UUID NATURAL_BLACK = UUID.fromString(
            "c0100000-0000-4000-8000-000000000001");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpUsers() {
        insertUser(USER_ID);
        insertUser(OTHER_USER_ID);
    }

    @Test
    void savesACandidateAndReadsItBackWithColorAndSignedCapture() throws Exception {
        UUID hairstyleId = insertHairstyle("남자 다운펌", null);
        UUID captureId = readyCapture(USER_ID);

        String created = mockMvc.perform(post("/me/saved-styles")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hairstyle_id":"%s","color_id":"%s","capture_id":"%s",
                                 "memo":"앞머리 살짝"}
                                """.formatted(hairstyleId, NATURAL_BLACK, captureId)))
                .andExpect(status().isCreated())
                // 이름은 카탈로그에서 그대로 옮겨 온다. 클라이언트가 보내지 않는다.
                .andExpect(jsonPath("$.data.style_name").value("남자 다운펌"))
                .andExpect(jsonPath("$.data.hairstyle_id").value(hairstyleId.toString()))
                .andExpect(jsonPath("$.data.color.name").value("내추럴 블랙"))
                .andExpect(jsonPath("$.data.color.hex_code").value("#1C1C1C"))
                .andExpect(jsonPath("$.data.memo").value("앞머리 살짝"))
                .andReturn().getResponse().getContentAsString();
        String savedStyleId = new ObjectMapper()
                .readTree(created).path("data").path("saved_style_id").asText();

        mockMvc.perform(get("/me/saved-styles")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].saved_style_id").value(savedStyleId))
                // URL 은 저장하지 않고 조회할 때마다 새로 서명한다.
                .andExpect(jsonPath("$.data.items[0].image_url",
                        containsString("X-Amz-Signature")));
    }

    @Test
    void fallsBackToTheCatalogThumbnailWhenThereIsNoCapture() throws Exception {
        UUID thumbnailFileId = readyCapture(null);
        UUID hairstyleId = insertHairstyle("레이어드 컷", thumbnailFileId);

        mockMvc.perform(post("/me/saved-styles")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hairstyle_id\":\"%s\"}".formatted(hairstyleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.color").doesNotExist())
                .andExpect(jsonPath("$.data.image_url", containsString("X-Amz-Signature")));
    }

    @Test
    void refusesCatalogEntriesAndCapturesThatAreNotUsable() throws Exception {
        UUID hairstyleId = insertHairstyle("숏 허쉬컷", null);

        // 카탈로그에서 내려간 스타일
        UUID retired = insertHairstyle("단종 스타일", null);
        jdbcTemplate.update("UPDATE hairstyle_assets SET active = FALSE WHERE hairstyle_id = ?",
                retired);
        mockMvc.perform(post("/me/saved-styles")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hairstyle_id\":\"%s\"}".formatted(retired)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        // 남의 캡처
        mockMvc.perform(post("/me/saved-styles")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hairstyle_id\":\"%s\",\"capture_id\":\"%s\"}"
                                .formatted(hairstyleId, readyCapture(OTHER_USER_ID))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_RESOURCE"));

        // 업로드가 끝나지 않은 캡처
        mockMvc.perform(post("/me/saved-styles")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hairstyle_id\":\"%s\",\"capture_id\":\"%s\"}"
                                .formatted(hairstyleId, pendingCapture(USER_ID))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("FILE_INVALID_STATE"));
    }

    @Test
    void deletesOwnCandidateAndDropsItFromSharesWithoutBreakingTheLink() throws Exception {
        UUID hairstyleId = insertHairstyle("남자 다운펌", null);
        UUID captureId = readyCapture(USER_ID);
        String created = mockMvc.perform(post("/me/saved-styles")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hairstyle_id\":\"%s\",\"capture_id\":\"%s\"}"
                                .formatted(hairstyleId, captureId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID savedStyleId = UUID.fromString(new ObjectMapper()
                .readTree(created).path("data").path("saved_style_id").asText());
        UUID shareId = insertShareCarrying(savedStyleId);

        mockMvc.perform(delete("/me/saved-styles/" + savedStyleId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNoContent());

        // 공유 자체는 남고 사라진 후보만 빠진다.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM shares WHERE share_id = ?", Integer.class, shareId))
                .isOne();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM share_saved_styles WHERE saved_style_id = ?",
                Integer.class, savedStyleId)).isZero();
        // 캡처 파일은 정리 대상으로 표시된다.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM files WHERE file_id = ?", String.class, captureId))
                .isEqualTo("DELETED");
    }

    @Test
    void hidesAnotherUsersCandidateBehindTheSameNotFound() throws Exception {
        UUID hairstyleId = insertHairstyle("남자 다운펌", null);
        String created = mockMvc.perform(post("/me/saved-styles")
                        .with(authentication(userAuthentication(OTHER_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hairstyle_id\":\"%s\"}".formatted(hairstyleId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String foreignId = new ObjectMapper()
                .readTree(created).path("data").path("saved_style_id").asText();

        mockMvc.perform(delete("/me/saved-styles/" + foreignId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/me/saved-styles")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void requiresAuthenticationForEveryEndpoint() throws Exception {
        mockMvc.perform(get("/me/saved-styles"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/me/saved-styles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hairstyle_id\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/me/saved-styles/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private UUID insertShareCarrying(UUID savedStyleId) {
        UUID shareId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO shares (share_id, user_id, token_hash, status, expires_at, created_at)
                VALUES (?, ?, ?, 'ACTIVE', now() + interval '1 day', now())
                """, shareId, USER_ID, "hash-" + shareId);
        jdbcTemplate.update(
                "INSERT INTO share_saved_styles (share_id, saved_style_id) VALUES (?, ?)",
                shareId, savedStyleId);
        return shareId;
    }

    private UUID insertHairstyle(String styleName, UUID thumbnailFileId) {
        UUID hairstyleId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO hairstyle_assets (
                    hairstyle_id, style_name, category, thumbnail_file_id, asset_version
                ) VALUES (?, ?, 'CUT', ?, 'v1')
                """, hairstyleId, styleName, thumbnailFileId);
        return hairstyleId;
    }

    private UUID readyCapture(UUID ownerId) {
        return insertCapture(ownerId, "READY");
    }

    private UUID pendingCapture(UUID ownerId) {
        return insertCapture(ownerId, "PENDING");
    }

    private UUID insertCapture(UUID ownerId, String status) {
        UUID fileId = UUID.randomUUID();
        String ownerType = ownerId == null ? "SYSTEM" : "USER";
        jdbcTemplate.update("""
                INSERT INTO files (
                    file_id, upload_id, user_id, owner_type, purpose, status, object_key,
                    content_type, file_name, file_size, expires_at
                ) VALUES (?, ?, ?, ?, 'AR_CAPTURE', ?, ?, 'image/jpeg', 'capture.jpg',
                          1024, now() + interval '5 minutes')
                """, fileId, UUID.randomUUID(), ownerId, ownerType, status,
                "AR_CAPTURE/" + ownerId + "/" + fileId);
        return fileId;
    }

    private void insertUser(UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, 'hash', 'EMAIL', 'ACTIVE', 0)
                """, userId, userId + "@example.com");
    }

    private UsernamePasswordAuthenticationToken userAuthentication(UUID userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }
}
