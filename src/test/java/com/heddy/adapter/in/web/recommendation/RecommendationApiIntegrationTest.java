package com.heddy.adapter.in.web.recommendation;

import com.heddy.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
class RecommendationApiIntegrationTest extends PostgresIntegrationTest {
    @TestConfiguration(proxyBeanMethods = false)
    static class OfflineSigningConfig {
        @Bean @Primary
        AwsCredentialsProvider offlineAwsCredentialsProvider() {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test-access-key", "test-secret-key"));
        }
    }

    private static final UUID USER_ID = UUID.fromString("93000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("93000000-0000-4000-8000-000000000002");
    private static final UUID PREFERRED_TAG_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.heddy.domain.recommendation.port.out.PhotoHairLengthPort photoAnalyzer;

    private UUID treatmentRecordId;

    @BeforeEach
    void setUp() {
        insertUser(USER_ID);
        insertUser(OTHER_USER_ID);
        jdbcTemplate.update("""
                INSERT INTO hair_profiles (hair_profile_id, user_id, hair_type, hair_condition,
                    hair_length, hair_thickness, available_care_time_minutes)
                VALUES (?, ?, 'WAVY', 'HEALTHY', 'BELOW_SHOULDER', 'NORMAL', 15)
                """, UUID.randomUUID(), USER_ID);
        jdbcTemplate.update("""
                INSERT INTO user_style_preferences
                    (preference_id, user_id, style_tag_id, preference_type)
                VALUES (?, ?, ?, 'PREFERRED')
                """, UUID.randomUUID(), USER_ID, PREFERRED_TAG_ID);
        treatmentRecordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_records
                    (record_id, user_id, service_types, performed_at, satisfaction)
                VALUES (?, ?, '["CUT"]'::jsonb, now() - interval '1 day', 5)
                """, treatmentRecordId, USER_ID);
        insertStyle("94000000-0000-4000-8000-000000000001", "레이어드 C컬",
                "MEDIUM", "OVERLAY", 30, true);
        insertStyle("94000000-0000-4000-8000-000000000002", "내추럴 보브",
                "BOB", "OVERLAY", 20, false);
        insertStyle("94000000-0000-4000-8000-000000000003", "소프트 웨이브",
                "LONG", "NONE", 10, false);
    }

    @Test
    void generatesReusesAndProtectsRuleBasedRecommendation() throws Exception {
        String response = mockMvc.perform(post("/recommendations/generate")
                        .with(authentication(auth(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"force_refresh\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.strategy").value("RULE_BASED_V1"))
                .andExpect(jsonPath("$.data.fallback").value(false))
                .andExpect(jsonPath("$.data.recommendation_basis.treatment_history.count").value(1))
                .andExpect(jsonPath("$.data.recommendation_basis.treatment_history"
                        + ".highest_satisfaction").value(5))
                .andExpect(jsonPath("$.data.recommendation_basis.ar_candidate_style_count").value(2))
                .andExpect(jsonPath("$.data.recommendation_basis.style_preferences"
                        + ".preferred_count").value(1))
                .andExpect(jsonPath("$.data.recommendation_basis.style_preferences"
                        + ".excluded_count").value(0))
                .andExpect(jsonPath("$.data.recommendation_basis.current_hair.hair_type")
                        .value("WAVY"))
                .andExpect(jsonPath("$.data.recommendation_basis.current_hair.hair_condition")
                        .value("HEALTHY"))
                .andExpect(jsonPath("$.data.recommendation_basis.current_hair.hair_length")
                        .value("BELOW_SHOULDER"))
                .andExpect(jsonPath("$.data.recommendation_basis.current_hair.hair_thickness")
                        .value("NORMAL"))
                .andExpect(jsonPath("$.data.recommendation_basis"
                        + ".available_care_time_minutes").value(15))
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].hairstyle.style_name").value("레이어드 C컬"))
                .andExpect(jsonPath("$.data.items[0].hairstyle.thumbnail_url").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].hairstyle.ar_mode").value("OVERLAY"))
                .andExpect(jsonPath("$.data.items[0].reasons[*].code")
                        .value(org.hamcrest.Matchers.hasItem("PREFERRED_TAG_MATCH")))
                .andReturn().getResponse().getContentAsString();

        String runId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response)
                .path("data").path("recommendation_run_id").asText();

        mockMvc.perform(post("/recommendations/generate")
                        .with(authentication(auth(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"force_refresh\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendation_run_id").value(runId));

        Integer runCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM recommendation_runs WHERE user_id = ?", Integer.class, USER_ID);
        assertThat(runCount).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM recommendation_items WHERE recommendation_run_id = ?",
                Integer.class, UUID.fromString(runId))).isEqualTo(3);

        jdbcTemplate.update("UPDATE hair_profiles SET available_care_time_minutes = 3 WHERE user_id = ?",
                USER_ID);
        jdbcTemplate.update("DELETE FROM user_style_preferences WHERE user_id = ?", USER_ID);

        mockMvc.perform(get("/recommendations/{id}", runId)
                        .with(authentication(auth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendation_basis.style_preferences"
                        + ".preferred_count").value(1))
                .andExpect(jsonPath("$.data.recommendation_basis"
                        + ".available_care_time_minutes").value(15));

        mockMvc.perform(get("/recommendations/{id}", runId)
                        .with(authentication(auth(OTHER_USER_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingReferencedTreatmentMarksRunStale() throws Exception {
        mockMvc.perform(post("/recommendations/generate")
                        .with(authentication(auth(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/treatment-records/{id}", treatmentRecordId)
                        .with(authentication(auth(USER_ID))))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM recommendation_runs WHERE user_id = ?", String.class, USER_ID))
                .isEqualTo("STALE");
        mockMvc.perform(get("/recommendations/latest").with(authentication(auth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.recommendation_basis.treatment_history.count").value(0));
    }

    @Test
    void latestRegeneratesOldAlgorithmAndThenReusesTheCorrectedRun() throws Exception {
        String oldId = generateRun();
        jdbcTemplate.update("UPDATE recommendation_runs SET input_hash = ? WHERE recommendation_run_id = ?",
                "0".repeat(64), UUID.fromString(oldId));
        entityManager.clear();

        String currentId = latestRun();
        assertThat(currentId).isNotEqualTo(oldId);
        assertThat(latestRun()).isEqualTo(currentId);
        assertThat(runCount()).isEqualTo(2);
    }

    @Test
    void latestReplacesLongStylesAfterProfileChangesToShort() throws Exception {
        String oldId = generateRun();
        insertShortStyle();
        jdbcTemplate.update("UPDATE hair_profiles SET hair_length = 'SHORT' WHERE user_id = ?", USER_ID);
        entityManager.clear();

        String response = mockMvc.perform(get("/recommendations/latest").with(authentication(auth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendation_basis.current_hair.hair_length").value("SHORT"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].hairstyle.style_name").value("클린 픽시컷"))
                .andReturn().getResponse().getContentAsString();
        String currentId = runId(response);
        assertThat(currentId).isNotEqualTo(oldId);
        assertThat(latestRun()).isEqualTo(currentId);
        assertThat(runCount()).isEqualTo(2);
        mockMvc.perform(get("/recommendations/{id}", oldId).with(authentication(auth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].hairstyle.style_name").value("레이어드 C컬"));
    }

    @Test
    void latestReevaluatesCatalogLengthsEvenWithoutMetadataVersionChange() throws Exception {
        String oldId = generateRun();
        jdbcTemplate.update("""
                UPDATE hairstyle_recommendation_profiles
                SET compatible_hair_lengths = '["BELOW_CHEST"]'::jsonb
                WHERE hairstyle_id = ?
                """, UUID.fromString("94000000-0000-4000-8000-000000000001"));
        entityManager.clear();

        String response = mockMvc.perform(get("/recommendations/latest").with(authentication(auth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[*].hairstyle.style_name")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("레이어드 C컬"))))
                .andReturn().getResponse().getContentAsString();
        assertThat(runId(response)).isNotEqualTo(oldId);
    }

    @Test
    void latestDoesNotReturnIncompatibleOldResultWhenNoCandidatesRemain() throws Exception {
        generateRun();
        jdbcTemplate.update("UPDATE hair_profiles SET hair_length = 'SHORT' WHERE user_id = ?", USER_ID);
        entityManager.clear();

        mockMvc.perform(get("/recommendations/latest").with(authentication(auth(USER_ID))))
                .andExpect(status().is(422));
        assertThat(runCount()).isEqualTo(1);
    }

    @Test
    void latestWithoutAnyPreviousRunStillReturnsNotFound() throws Exception {
        mockMvc.perform(get("/recommendations/latest").with(authentication(auth(USER_ID))))
                .andExpect(status().isNotFound());
        assertThat(runCount()).isZero();
    }

    @Test
    void forcedRefreshStillCreatesANewRunAndFiltersLongStyles() throws Exception {
        insertShortStyle();
        jdbcTemplate.update("UPDATE hair_profiles SET hair_length = 'SHORT' WHERE user_id = ?", USER_ID);
        String oldId = generateRun();
        String response = mockMvc.perform(post("/recommendations/generate")
                        .with(authentication(auth(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"force_refresh\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].hairstyle.style_name").value("클린 픽시컷"))
                .andReturn().getResponse().getContentAsString();
        assertThat(runId(response)).isNotEqualTo(oldId);
        assertThat(runCount()).isEqualTo(2);
    }

    private void insertShortStyle() {
        String id = "94000000-0000-4000-8000-000000000004";
        insertStyle(id, "클린 픽시컷", "SHORT", "NONE", 60, false);
        jdbcTemplate.update("""
                UPDATE hairstyle_recommendation_profiles
                SET compatible_hair_lengths = '["SHORT", "BELOW_CHIN"]'::jsonb
                WHERE hairstyle_id = ?
                """, UUID.fromString(id));
    }

    @Test
    void auditedCatalogMigrationRemovesImpossibleLengthsAndAmbiguousReferences() {
        insertStyle("94000000-0000-4000-8000-000000000004", "클래식 보브", "BOB", "NONE", 1, false);
        insertStyle("94000000-0000-4000-8000-000000000005", "컬리 스타일 08", "CURLY", "NONE", 1, false);
        insertStyle("94000000-0000-4000-8000-000000000006", "숏 스타일 05", "SHORT", "NONE", 1, false);
        insertStyle("94000000-0000-4000-8000-000000000007", "미디엄 스타일 32", "MEDIUM", "NONE", 1, false);
        jdbcTemplate.update("UPDATE hairstyle_recommendation_profiles SET compatible_hair_lengths = '[\"SHORT\",\"BELOW_CHIN\",\"BELOW_SHOULDER\"]'::jsonb");
        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                    new org.springframework.core.io.ClassPathResource("db/migration/V33__correct_audited_short_catalog.sql"));
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                    new org.springframework.core.io.ClassPathResource("db/migration/V34__exclude_mislabeled_braided_reference.sql"));
            return null;
        });
        assertThat(jdbcTemplate.queryForObject("SELECT minimum_hair_length FROM hairstyle_recommendation_profiles WHERE hairstyle_id = '94000000-0000-4000-8000-000000000004'", String.class))
                .isEqualTo("BELOW_CHIN");
        assertThat(jdbcTemplate.queryForObject("SELECT compatible_hair_lengths::text FROM hairstyle_recommendation_profiles WHERE hairstyle_id = '94000000-0000-4000-8000-000000000004'", String.class))
                .doesNotContain("SHORT");
        assertThat(jdbcTemplate.queryForObject("SELECT minimum_hair_length FROM hairstyle_recommendation_profiles WHERE hairstyle_id = '94000000-0000-4000-8000-000000000005'", String.class))
                .isEqualTo("BELOW_SHOULDER");
        assertThat(jdbcTemplate.queryForObject("SELECT active FROM hairstyle_assets WHERE hairstyle_id = '94000000-0000-4000-8000-000000000006'", Boolean.class)).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT active FROM hairstyle_assets WHERE hairstyle_id = '94000000-0000-4000-8000-000000000007'", Boolean.class)).isFalse();
    }

    @Test
    void missingLengthDoesNotReturnArbitraryLongHair() throws Exception {
        mockMvc.perform(post("/recommendations/generate").with(authentication(auth(OTHER_USER_ID)))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("RECOMMENDATION_HAIR_LENGTH_REQUIRED"));
    }

    @Test
    void newPhotoChangesLatestRecommendationAndPersistsAnalysisEvidence() throws Exception {
        String previous = generateRun();
        insertStyle("94000000-0000-4000-8000-000000000004", "짧은 테스트 컷", "SHORT", "NONE", 1, false);
        jdbcTemplate.update("UPDATE hairstyle_recommendation_profiles SET compatible_hair_lengths = '[\"SHORT\"]'::jsonb WHERE hairstyle_id = ?",
                UUID.fromString("94000000-0000-4000-8000-000000000004"));
        UUID fileId = attachAfterPhoto();
        org.mockito.Mockito.when(photoAnalyzer.analyze(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(i -> new com.heddy.domain.recommendation.model.PhotoHairEvidence(
                        ((com.heddy.domain.file.model.StoredFile) i.getArgument(0)).fileId(), "test-model", "ASSESSED",
                        com.heddy.domain.account.model.HairProfile.HairLength.SHORT, false));
        entityManager.clear();
        String body = mockMvc.perform(get("/recommendations/latest").with(authentication(auth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].hairstyle.style_name").value("짧은 테스트 컷"))
                .andExpect(jsonPath("$.data.recommendation_basis.photo_analysis.file_id").value(fileId.toString()))
                .andExpect(jsonPath("$.data.recommendation_basis.photo_analysis.applied").value(true))
                .andExpect(jsonPath("$.data.recommendation_basis.current_hair.hair_length").value("SHORT"))
                .andReturn().getResponse().getContentAsString();
        String updated = runId(body);
        assertThat(updated).isNotEqualTo(previous);
        entityManager.clear();
        assertThat(latestRun()).isEqualTo(updated);
        assertThat(jdbcTemplate.queryForObject("SELECT hair_length FROM hair_profiles WHERE user_id = ?", String.class, USER_ID))
                .isEqualTo("BELOW_SHOULDER");
        // Replacing the file behind the same photo ID must also invalidate a cached run.
        UUID replacement = attachAfterPhoto();
        jdbcTemplate.update("DELETE FROM treatment_record_photos WHERE record_id = ? AND file_id = ?", treatmentRecordId, replacement);
        jdbcTemplate.update("UPDATE treatment_record_photos SET file_id = ? WHERE record_id = ? AND file_id = ?",
                replacement, treatmentRecordId, fileId);
        entityManager.clear();
        assertThat(latestRun()).isNotEqualTo(updated);
    }

    @Test
    void newestUploadOnLatestSameDayRecordBecomesTheAnalysisInput() throws Exception {
        UUID olderFile = attachAfterPhoto();
        UUID newerFile = attachAfterPhoto();
        jdbcTemplate.update("UPDATE files SET created_at = now() - interval '1 hour' WHERE file_id = ?", olderFile);
        // The newest record deliberately has the smallest UUID; UUID ordering must not select old photos.
        UUID latestRecord = UUID.fromString("00000000-0000-4000-8000-000000000001");
        jdbcTemplate.update("UPDATE treatment_records SET created_at = now() - interval '1 hour' WHERE record_id = ?", treatmentRecordId);
        jdbcTemplate.update("""
                INSERT INTO treatment_records (record_id, user_id, service_types, performed_at, satisfaction)
                SELECT ?, user_id, service_types, performed_at, satisfaction FROM treatment_records WHERE record_id = ?
                """, latestRecord, treatmentRecordId);
        jdbcTemplate.update("UPDATE treatment_record_photos SET record_id = ? WHERE record_id = ?", latestRecord, treatmentRecordId);
        org.mockito.Mockito.when(photoAnalyzer.analyze(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(i -> new com.heddy.domain.recommendation.model.PhotoHairEvidence(
                        ((com.heddy.domain.file.model.StoredFile) i.getArgument(0)).fileId(), "test-model", "UNCERTAIN", null, false));
        entityManager.clear();
        mockMvc.perform(post("/recommendations/generate").with(authentication(auth(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendation_basis.photo_analysis.file_id").value(newerFile.toString()));
        org.mockito.Mockito.verify(photoAnalyzer).analyze(org.mockito.ArgumentMatchers.argThat(f -> f.fileId().equals(newerFile)));
    }

    private UUID attachAfterPhoto() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("UPDATE hair_profiles SET updated_at = now() - interval '2 days' WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("""
                INSERT INTO files (file_id, upload_id, user_id, purpose, status, object_key,
                    content_type, file_name, file_size, expires_at)
                VALUES (?, ?, ?, 'TREATMENT_PHOTO', 'READY', ?, 'image/jpeg', 'after.jpg', 1024, now() + interval '1 day')
                """, id, UUID.randomUUID(), USER_ID, "test/" + id);
        jdbcTemplate.update("""
                INSERT INTO treatment_record_photos (photo_id, record_id, file_id, image_type, sort_order)
                VALUES (?, ?, ?, 'AFTER', 0)
                """, UUID.randomUUID(), treatmentRecordId, id);
        return id;
    }

    private String generateRun() throws Exception {
        return runId(mockMvc.perform(post("/recommendations/generate")
                        .with(authentication(auth(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private String latestRun() throws Exception {
        return runId(mockMvc.perform(get("/recommendations/latest").with(authentication(auth(USER_ID))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private String runId(String response) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response)
                .path("data").path("recommendation_run_id").asText();
    }

    private int runCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM recommendation_runs WHERE user_id = ?",
                Integer.class, USER_ID);
    }

    @Test
    void legacyRunWithoutRecommendationBasisIsReadableButNotReusable() throws Exception {
        String firstResponse = mockMvc.perform(post("/recommendations/generate")
                        .with(authentication(auth(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String firstRunId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(firstResponse)
                .path("data").path("recommendation_run_id").asText();

        jdbcTemplate.update("""
                UPDATE recommendation_runs
                SET input_snapshot_json = input_snapshot_json - 'recommendation_basis'
                WHERE recommendation_run_id = ?
                """, UUID.fromString(firstRunId));
        entityManager.clear();

        mockMvc.perform(get("/recommendations/{id}", firstRunId)
                        .with(authentication(auth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendation_basis").doesNotExist());

        String regeneratedResponse = mockMvc.perform(post("/recommendations/generate")
                        .with(authentication(auth(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendation_basis").exists())
                .andReturn().getResponse().getContentAsString();
        String regeneratedRunId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(regeneratedResponse).path("data").path("recommendation_run_id").asText();

        assertThat(regeneratedRunId).isNotEqualTo(firstRunId);
    }

    private void insertStyle(
            String hairstyleIdText,
            String name,
            String category,
            String arMode,
            int priority,
            boolean tagged
    ) {
        UUID hairstyleId = UUID.fromString(hairstyleIdText);
        UUID fileId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO files (file_id, upload_id, user_id, owner_type, purpose, status, object_key,
                    content_type, file_name, file_size, expires_at)
                VALUES (?, ?, NULL, 'SYSTEM', 'HAIRSTYLE_THUMBNAIL', 'READY', ?, 'image/jpeg', ?, 1024,
                    now() + interval '1 day')
                """, fileId, UUID.randomUUID(), "catalog/" + fileId, name + ".jpg");
        jdbcTemplate.update("""
                INSERT INTO hairstyle_assets (hairstyle_id, style_name, category,
                    thumbnail_file_id, ar_mode, active, asset_version)
                VALUES (?, ?, ?, ?, ?, true, '1.0.0')
                """, hairstyleId, name, category, fileId, arMode);
        jdbcTemplate.update("""
                INSERT INTO hairstyle_recommendation_profiles (
                    hairstyle_id, service_types, compatible_hair_lengths,
                    compatible_hair_types, compatible_hair_thicknesses,
                    compatible_hair_conditions, contraindicated_hair_conditions,
                    estimated_daily_care_minutes, management_difficulty,
                    chemical_stress_level, editorial_priority, metadata_version)
                VALUES (?, '["CUT"]'::jsonb, '["BELOW_SHOULDER"]'::jsonb,
                    '["WAVY"]'::jsonb, '["NORMAL"]'::jsonb, '["HEALTHY"]'::jsonb,
                    '[]'::jsonb, 10, 'MEDIUM', 'LOW', ?, '1')
                """, hairstyleId, priority);
        if (tagged) {
            jdbcTemplate.update("""
                    INSERT INTO hairstyle_style_tags (hairstyle_id, style_tag_id) VALUES (?, ?)
                    """, hairstyleId, PREFERRED_TAG_ID);
        }
    }

    private void insertUser(UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO users (user_id, email, password_hash, auth_provider, status, login_fail_count)
                VALUES (?, ?, 'hash', 'EMAIL', 'ACTIVE', 0)
                """, userId, userId + "@example.com");
    }

    private UsernamePasswordAuthenticationToken auth(UUID userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }
}
