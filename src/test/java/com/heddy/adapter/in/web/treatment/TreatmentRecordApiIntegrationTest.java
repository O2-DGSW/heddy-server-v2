package com.heddy.adapter.in.web.treatment;

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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 등록의 HTTP 계약을 Postgres 를 상대로 본다. 사진 파일은 LocalStack 흐름을 밟지 않고 files 행을
 * 직접 심어 READY·PENDING 상태를 만든다 — 이 테스트가 검증할 것은 file_id 참조 규칙이지
 * 업로드 프로토콜이 아니다.
 */
@Transactional
@AutoConfigureMockMvc
class TreatmentRecordApiIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString(
            "82000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "82000000-0000-4000-8000-000000000002");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpUsers() {
        insertUser(USER_ID);
        insertUser(OTHER_USER_ID);
    }

    // ------------------------------------------------------------------ 등록

    @Test
    void createsRecordWithoutPhotosAndAnswers201WithCoreFields() throws Exception {
        mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z", "[]"))
                        .header("X-Request-Id", "request-31"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.record_id").isNotEmpty())
                .andExpect(jsonPath("$.data.service_types", containsInAnyOrder("CUT", "COLOR")))
                .andExpect(jsonPath("$.data.salon_name").value("준헤어"))
                .andExpect(jsonPath("$.data.designer_name").value("김실장"))
                .andExpect(jsonPath("$.data.performed_at").value("2026-08-01T10:00:00Z"))
                .andExpect(jsonPath("$.data.satisfaction").value(4))
                .andExpect(jsonPath("$.data.price.amount").value(120_000))
                .andExpect(jsonPath("$.data.price.currency").value("KRW"))
                .andExpect(jsonPath("$.data.photos").isEmpty())
                .andExpect(jsonPath("$.request_id").value("request-31"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM treatment_records WHERE user_id = ?", Integer.class, USER_ID);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void echoesAttachedPhotosWithoutAnyUrl() throws Exception {
        UUID before = readyFile(USER_ID);
        UUID after = readyFile(USER_ID);

        mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z", photoRefs(before, after))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.photos", hasSize(2)))
                .andExpect(jsonPath("$.data.photos[0].photo_id").isNotEmpty())
                .andExpect(jsonPath("$.data.photos[0].image_type").value("BEFORE"))
                .andExpect(jsonPath("$.data.photos[0].photo_url").doesNotExist());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM treatment_record_photos p "
                        + "JOIN treatment_records r ON r.record_id = p.record_id "
                        + "WHERE r.user_id = ?", Integer.class, USER_ID);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void rejectsFuturePerformedAtWithTheDomainErrorCode() throws Exception {
        mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2099-01-01T00:00:00Z", "[]")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("TREATMENT_PERFORMED_AT_IN_FUTURE"));
    }

    @Test
    void rejectsEmptyServiceTypesAsFieldValidation() throws Exception {
        mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"service_types":[],"performed_at":"2026-08-01T10:00:00Z"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.field_errors[0].field").value("service_types"));
    }

    @Test
    void refusesPhotoFileOwnedBySomeoneElse() throws Exception {
        UUID foreignFile = readyFile(OTHER_USER_ID);

        mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z", photoRefs(foreignFile))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_RESOURCE"));
    }

    @Test
    void refusesPhotoFileThatHasNotFinishedUploading() throws Exception {
        UUID pendingFile = pendingFile(USER_ID);

        mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z", photoRefs(pendingFile))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("FILE_INVALID_STATE"));
    }

    @Test
    void refusesUnknownPhotoFile() throws Exception {
        mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z",
                                photoRefs(UUID.randomUUID()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/treatment-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z", "[]")))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ 문서화

    @Test
    void documentsTreatmentEndpointsInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/treatment-records']['post']"
                        + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$.components.schemas.CreateTreatmentRecordRequest"
                        + ".properties.service_types.description").value(containsString("CUT")))
                .andExpect(jsonPath("$.components.schemas.CreateTreatmentRecordRequest"
                        + ".properties.performed_at.description").value(containsString("미래")))
                .andExpect(jsonPath("$.components.schemas.CreateTreatmentRecordRequest.required",
                        hasItem("performed_at")))
                .andExpect(jsonPath("$.components.schemas.TreatmentRecordResponse"
                        + ".properties.price.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.TreatmentRecordResponse"
                        + ".properties.photos.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.TreatmentRecordResponse"
                        + ".properties.photos.items").exists());
    }

    // ------------------------------------------------------------------ 헬퍼

    private String createBody(String performedAt, String photosJson) {
        return """
                {"service_types":["CUT","COLOR"],"performed_at":"%s",
                 "salon_name":"준헤어","designer_name":"김실장","satisfaction":4,
                 "price_amount":120000,"price_currency":"KRW","photos":%s}
                """.formatted(performedAt, photosJson);
    }

    private String photoRefs(UUID... fileIds) {
        StringBuilder refs = new StringBuilder("[");
        for (int i = 0; i < fileIds.length; i++) {
            if (i > 0) {
                refs.append(",");
            }
            refs.append("""
                    {"file_id":"%s","image_type":"%s"}""".formatted(fileIds[i],
                    i == 0 ? "BEFORE" : "AFTER"));
        }
        return refs.append("]").toString();
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

    private UUID readyFile(UUID ownerId) {
        return insertFile(ownerId, "READY");
    }

    private UUID pendingFile(UUID ownerId) {
        return insertFile(ownerId, "PENDING");
    }

    private UUID insertFile(UUID ownerId, String status) {
        UUID fileId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO files (
                    file_id, upload_id, user_id, purpose, status, object_key,
                    content_type, file_name, file_size, expires_at
                ) VALUES (?, ?, ?, 'TREATMENT_PHOTO', ?, ?, 'image/jpeg', 'after.jpg',
                          1024, now() + interval '5 minutes')
                """, fileId, UUID.randomUUID(), ownerId, status,
                "TREATMENT_PHOTO/" + ownerId + "/" + fileId);
        return fileId;
    }
}
