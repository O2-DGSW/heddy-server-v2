package com.heddy.adapter.in.web.treatment;

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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 등록·단건 조회의 HTTP 계약을 Postgres 를 상대로 본다. 사진 파일은 LocalStack 흐름을 밟지 않고
 * files 행을 직접 심어 READY·PENDING 상태를 만든다 — 이 테스트가 검증할 것은 file_id 참조
 * 규칙과 Presigned GET 발급이지 업로드 프로토콜이 아니다.
 */
@Transactional
@AutoConfigureMockMvc
class TreatmentRecordApiIntegrationTest extends PostgresIntegrationTest {

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

    // ------------------------------------------------------------------ 조회

    @Test
    void listsOnlyOwnRecordsMatchingAllFiltersWithStablePagination() throws Exception {
        UUID older = insertRecord(USER_ID, "[\"CUT\"]", "준헤어", "김실장",
                "2026-08-10T10:00:00Z");
        UUID newer = insertRecord(USER_ID, "[\"CUT\",\"COLOR\"]", "준헤어", "김실장",
                "2026-08-20T10:00:00Z");
        insertRecord(USER_ID, "[\"PERM\"]", "준헤어", "김실장",
                "2026-08-15T10:00:00Z");
        insertRecord(USER_ID, "[\"CUT\"]", "다른미용실", "김실장",
                "2026-08-15T10:00:00Z");
        insertRecord(USER_ID, "[\"CUT\"]", "준헤어", "다른디자이너",
                "2026-08-15T10:00:00Z");
        insertRecord(USER_ID, "[\"CUT\"]", "준헤어", "김실장",
                "2026-07-01T10:00:00Z");
        insertRecord(OTHER_USER_ID, "[\"CUT\"]", "준헤어", "김실장",
                "2026-08-22T10:00:00Z");

        mockMvc.perform(get("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("service_type", "CUT")
                        .param("designer_name", "김실장")
                        .param("salon_name", "준헤어")
                        .param("from", "2026-08-01T00:00:00Z")
                        .param("to", "2026-08-31T23:59:59Z")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "performedAt,desc")
                        .header("X-Request-Id", "request-32"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].record_id").value(newer.toString()))
                .andExpect(jsonPath("$.data.items[0].service_types",
                        containsInAnyOrder("CUT", "COLOR")))
                .andExpect(jsonPath("$.data.page.number").value(0))
                .andExpect(jsonPath("$.data.page.size").value(1))
                .andExpect(jsonPath("$.data.page.total_elements").value(2))
                .andExpect(jsonPath("$.data.page.total_pages").value(2))
                .andExpect(jsonPath("$.data.page.has_next").value(true))
                .andExpect(jsonPath("$.request_id").value("request-32"));

        mockMvc.perform(get("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("service_type", "CUT")
                        .param("designer_name", "김실장")
                        .param("salon_name", "준헤어")
                        .param("from", "2026-08-01T00:00:00Z")
                        .param("to", "2026-08-31T23:59:59Z")
                        .param("page", "1")
                        .param("size", "1")
                        .param("sort", "performedAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].record_id").value(older.toString()))
                .andExpect(jsonPath("$.data.page.has_next").value(false));
    }

    @Test
    void answersAnEmptyPageInsteadOfNotFound() throws Exception {
        mockMvc.perform(get("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.page.number").value(0))
                .andExpect(jsonPath("$.data.page.size").value(20))
                .andExpect(jsonPath("$.data.page.total_elements").value(0))
                .andExpect(jsonPath("$.data.page.total_pages").value(0))
                .andExpect(jsonPath("$.data.page.has_next").value(false));
    }

    @Test
    void rejectsInvalidListRangePageAndSortAsBadRequest() throws Exception {
        mockMvc.perform(get("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("from", "2026-08-20T00:00:00Z")
                        .param("to", "2026-08-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void readsBackOwnRecordWithSignedPhotoUrlsIssuedAtReadTime() throws Exception {
        UUID before = readyFile(USER_ID);
        String created = mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z", photoRefs(before))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String recordId = new ObjectMapper()
                .readTree(created).path("data").path("record_id").asText();

        mockMvc.perform(get("/treatment-records/" + recordId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.record_id").value(recordId))
                .andExpect(jsonPath("$.data.service_types", hasSize(2)))
                .andExpect(jsonPath("$.data.photos", hasSize(1)))
                .andExpect(jsonPath("$.data.photos[0].photo_id").isNotEmpty())
                .andExpect(jsonPath("$.data.photos[0].image_type").value("BEFORE"))
                .andExpect(jsonPath("$.data.photos[0].photo_url",
                        containsString("X-Amz-Signature")));
    }

    /** 타인 기록은 존재 여부를 노출하지 않게 없는 기록과 같은 404 다(#31). 403 이 아니다. */
    @Test
    void hidesAnotherUsersRecordBehindResourceNotFound() throws Exception {
        String recordId = createRecord(USER_ID);

        mockMvc.perform(get("/treatment-records/" + recordId)
                        .with(authentication(userAuthentication(OTHER_USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message",
                        not(emptyOrNullString())));
    }

    @Test
    void answersResourceNotFoundForUnknownRecordId() throws Exception {
        mockMvc.perform(get("/treatment-records/" + UUID.randomUUID())
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void requiresAuthenticationForAllEndpoints() throws Exception {
        mockMvc.perform(post("/treatment-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z", "[]")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/treatment-records/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/treatment-records"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ 문서화

    @Test
    void documentsTreatmentEndpointsInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/treatment-records']['post']"
                        + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$['paths']['/treatment-records']['get']"
                        + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$['paths']['/treatment-records/{recordId}']['get']"
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

    private String createRecord(UUID userId) throws Exception {
        String created = mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z", "[]")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new ObjectMapper()
                .readTree(created).path("data").path("record_id").asText();
    }

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

    private UUID insertRecord(
            UUID userId,
            String serviceTypes,
            String salonName,
            String designerName,
            String performedAt
    ) {
        UUID recordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_records (
                    record_id, user_id, service_types, salon_name, designer_name, performed_at
                ) VALUES (?, ?, CAST(? AS jsonb), ?, ?, ?)
                """, recordId, userId, serviceTypes, salonName, designerName,
                Timestamp.from(Instant.parse(performedAt)));
        return recordId;
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
