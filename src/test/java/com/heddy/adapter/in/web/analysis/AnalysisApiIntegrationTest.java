package com.heddy.adapter.in.web.analysis;

import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 분석 조회의 HTTP 계약. 분석을 만드는 경로(요청 API·AI 서버 연동)가 아직 없어 행을 직접
 * 심는다 — 이 테스트가 볼 것은 조회 계약이지 분석 파이프라인이 아니다.
 */
@Transactional
@AutoConfigureMockMvc
class AnalysisApiIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("87000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("87000000-0000-4000-8000-000000000002");
    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    private UUID recordId;
    private UUID photoId;

    @BeforeEach
    void setUp() {
        insertUser(USER_ID, "analysis-api-owner@example.com");
        insertUser(OTHER_USER_ID, "analysis-api-other@example.com");
        recordId = insertRecord(USER_ID);
        photoId = insertPhoto(recordId);
    }

    @Test
    void returnsEveryMetricWithItsDirectionForTheDetailScreen() throws Exception {
        UUID jobId = insertJob("SUCCEEDED");
        insertResult(jobId);

        mockMvc.perform(get("/treatment-records/{recordId}/analyses/latest", recordId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.model_version").value("hair-v1.2.0"))
                .andExpect(jsonPath("$.data.confidence.score").value(82.40))
                .andExpect(jsonPath("$.data.confidence.grade").value("HIGH"))
                .andExpect(jsonPath("$.data.metrics", hasSize(4)))
                // 순서는 열거형 선언 순서로 고정된다. 화면 목록이 요청마다 뒤바뀌면 안 된다.
                .andExpect(jsonPath("$.data.metrics[0].type").value("COLOR_UNIFORMITY"))
                .andExpect(jsonPath("$.data.metrics[0].score").value(78.00))
                .andExpect(jsonPath("$.data.metrics[0].higher_is_better").value(true))
                .andExpect(jsonPath("$.data.metrics[3].type").value("ROUGHNESS"))
                // roughness 만 방향이 반대다. 이게 뒤집히면 화면 게이지가 거꾸로 그려진다.
                .andExpect(jsonPath("$.data.metrics[3].higher_is_better").value(false))
                .andExpect(jsonPath("$.data.overlays", hasSize(0)));
    }

    /** 사진이 바뀐 뒤에도 결과는 내려가고 상태로만 알린다. */
    @Test
    void reportsStaleWithoutHidingTheResult() throws Exception {
        UUID jobId = insertJob("STALE");
        insertResult(jobId);

        mockMvc.perform(get("/treatment-records/{recordId}/analyses/latest", recordId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("STALE"))
                .andExpect(jsonPath("$.data.analysis_id").isNotEmpty());
    }

    @Test
    void answersNotFoundWhenTheRecordWasNeverAnalysed() throws Exception {
        mockMvc.perform(get("/treatment-records/{recordId}/analyses/latest", recordId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    /** 남의 기록은 분석이 있어도 없는 기록과 같은 404 다. */
    @Test
    void hidesAnalysesOfSomeoneElsesRecord() throws Exception {
        UUID jobId = insertJob("SUCCEEDED");
        insertResult(jobId);

        mockMvc.perform(get("/treatment-records/{recordId}/analyses/latest", recordId)
                        .with(authentication(userAuthentication(OTHER_USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/treatment-records/{recordId}/analyses/latest", recordId))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ 헬퍼

    private UsernamePasswordAuthenticationToken userAuthentication(UUID userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private UUID insertJob(String status) {
        UUID jobId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO analysis_jobs (
                    job_id, user_id, record_id, photo_id, status, progress, attempt_count
                ) VALUES (?, ?, ?, ?, ?, 100, 1)
                """, jobId, USER_ID, recordId, photoId, status);
        return jobId;
    }

    private void insertResult(UUID jobId) {
        jdbcTemplate.update("""
                INSERT INTO analysis_results (
                    analysis_id, job_id, user_id, record_id, photo_id,
                    color_uniformity_score, color_uniformity_grade,
                    shape_symmetry_score, shape_symmetry_grade,
                    volume_balance_score, volume_balance_grade,
                    roughness_score, roughness_grade,
                    confidence_score, confidence_grade,
                    model_version, summary, analyzed_at
                ) VALUES (?, ?, ?, ?, ?, 78.00, 'HIGH', 71.00, 'HIGH', 64.00, 'MEDIUM',
                          41.00, 'LOW', 82.40, 'HIGH', 'hair-v1.2.0', ?, ?)
                """, UUID.randomUUID(), jobId, USER_ID, recordId, photoId,
                "사진에서 거칠게 보이는 영역이 감지되었습니다", Timestamp.from(NOW));
    }

    private void insertUser(UUID userId, String email) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, ?, 'EMAIL', 'ACTIVE', 0)
                """, userId, email, "hash");
    }

    private UUID insertRecord(UUID ownerId) {
        UUID newRecordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_records (record_id, user_id, service_types, performed_at)
                VALUES (?, ?, CAST(? AS jsonb), ?)
                """, newRecordId, ownerId, "[\"CUT\"]", Timestamp.from(NOW.minusSeconds(3600)));
        return newRecordId;
    }

    private UUID insertPhoto(UUID ownerRecordId) {
        UUID fileId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO files (
                    file_id, upload_id, user_id, purpose, status, object_key,
                    content_type, file_name, file_size, expires_at
                ) VALUES (?, ?, ?, 'TREATMENT_PHOTO', 'READY', ?, 'image/jpeg', 'photo.jpg', 1024,
                          now() + interval '5 minutes')
                """, fileId, UUID.randomUUID(), USER_ID, "TREATMENT_PHOTO/" + USER_ID + "/" + fileId);
        UUID newPhotoId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_record_photos (photo_id, record_id, file_id, image_type)
                VALUES (?, ?, ?, 'AFTER')
                """, newPhotoId, ownerRecordId, fileId);
        return newPhotoId;
    }
}
