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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                .andExpect(jsonPath("$.data.memo").value("기존 메모"))
                .andExpect(jsonPath("$.data.next_visit_cautions").value("기존 주의사항"))
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
    void rejectsNullServiceTypeElementAsClientError() throws Exception {
        mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"service_types":[null],"performed_at":"2026-08-01T10:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsNullPhotoElementAsFieldValidation() throws Exception {
        mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z", "[null]")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.field_errors[0].field",
                        containsString("photos")));
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

    /** 대표 사진은 생성 순서상 첫 번째로 조회 가능한 사진이고, 준비 상태가 아니면 null 이다. */
    @Test
    void listsRecordsWithTheFirstAvailableReadyThumbnailOrNone() throws Exception {
        UUID pending = pendingFile(USER_ID);
        UUID ready = readyFile(USER_ID);
        UUID deleted = insertFile(USER_ID, "DELETED");
        UUID newer = insertRecord(USER_ID, "[\"CUT\"]", "준헤어", "김실장",
                "2026-08-20T10:00:00Z");
        insertPhotoRow(newer, pending, "BEFORE", 0);
        insertPhotoRow(newer, ready, "AFTER", 1);
        UUID older = insertRecord(USER_ID, "[\"CUT\"]", "준헤어", "김실장",
                "2026-08-10T10:00:00Z");
        insertPhotoRow(older, deleted, "BEFORE", 0);

        mockMvc.perform(get("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("sort", "performedAt,desc")
                        .header("X-Request-Id", "request-66"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].record_id").value(newer.toString()))
                .andExpect(jsonPath("$.data.items[0].thumbnail_url",
                        containsString("X-Amz-Signature")))
                .andExpect(jsonPath("$.data.items[1].record_id").value(older.toString()))
                .andExpect(jsonPath("$.data.items[1].thumbnail_url").value(nullValue()));
    }

    /**
     * "공유중" 배지. 살아 있는 공유에 담긴 기록만 true 이고, 철회·만료된 공유는 배지를 켜지
     * 못한다 — 만료를 상태로 저장하지 않으므로 status 만 보면 만료된 링크가 남는다.
     */
    @Test
    void listsWhetherEachRecordIsCurrentlyShared() throws Exception {
        UUID shared = insertRecord(USER_ID, "[\"CUT\"]", null, null, "2026-08-20T10:00:00Z");
        UUID expired = insertRecord(USER_ID, "[\"CUT\"]", null, null, "2026-08-15T10:00:00Z");
        UUID revoked = insertRecord(USER_ID, "[\"CUT\"]", null, null, "2026-08-10T10:00:00Z");
        UUID never = insertRecord(USER_ID, "[\"CUT\"]", null, null, "2026-08-05T10:00:00Z");
        insertShare(USER_ID, shared, "ACTIVE", Instant.now().plusSeconds(3600));
        insertShare(USER_ID, expired, "ACTIVE", Instant.now().minusSeconds(1));
        insertShare(USER_ID, revoked, "REVOKED", Instant.now().plusSeconds(3600));

        mockMvc.perform(get("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("sort", "performedAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(4)))
                .andExpect(jsonPath("$.data.items[0].record_id").value(shared.toString()))
                .andExpect(jsonPath("$.data.items[0].is_shared").value(true))
                .andExpect(jsonPath("$.data.items[1].record_id").value(expired.toString()))
                .andExpect(jsonPath("$.data.items[1].is_shared").value(false))
                .andExpect(jsonPath("$.data.items[2].record_id").value(revoked.toString()))
                .andExpect(jsonPath("$.data.items[2].is_shared").value(false))
                .andExpect(jsonPath("$.data.items[3].record_id").value(never.toString()))
                .andExpect(jsonPath("$.data.items[3].is_shared").value(false));
    }

    /**
     * "분석 완료" 배지. 서버는 상태를 그대로 내려주고 배지 문구는 클라이언트가 정한다 —
     * 완료는 SUCCEEDED 만 해당하고, 사진이 바뀐 뒤의 STALE 을 완료로 묶으면 옛 결과가
     * 완료로 보인다.
     */
    @Test
    void listsTheLatestAnalysisStatusOfEachRecord() throws Exception {
        UUID succeeded = insertRecord(USER_ID, "[\"CUT\"]", null, null, "2026-08-20T10:00:00Z");
        UUID stale = insertRecord(USER_ID, "[\"CUT\"]", null, null, "2026-08-15T10:00:00Z");
        UUID neverAnalysed = insertRecord(USER_ID, "[\"CUT\"]", null, null, "2026-08-10T10:00:00Z");
        insertAnalysisJob(succeeded, "SUCCEEDED");
        insertAnalysisJob(stale, "STALE");

        mockMvc.perform(get("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .param("sort", "performedAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(3)))
                .andExpect(jsonPath("$.data.items[0].record_id").value(succeeded.toString()))
                .andExpect(jsonPath("$.data.items[0].analysis_status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.items[1].record_id").value(stale.toString()))
                .andExpect(jsonPath("$.data.items[1].analysis_status").value("STALE"))
                .andExpect(jsonPath("$.data.items[2].record_id").value(neverAnalysed.toString()))
                .andExpect(jsonPath("$.data.items[2].analysis_status").value(nullValue()));
    }

    /** 기록 추가 화면의 "소요 시간"·"시술 내용" 입력란이 실제로 저장되고 조회된다. */
    @Test
    void storesAndReturnsDurationAndTreatmentContent() throws Exception {
        String created = mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "service_types": ["COLOR"],
                                  "performed_at": "2026-08-20T10:00:00Z",
                                  "duration_minutes": 90,
                                  "treatment_content": "애쉬브라운 전체 염색",
                                  "memo": "개인 메모"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.duration_minutes").value(90))
                .andExpect(jsonPath("$.data.treatment_content").value("애쉬브라운 전체 염색"))
                // 시술 내용과 메모는 별개 필드다. 한쪽이 다른 쪽을 덮어쓰면 안 된다.
                .andExpect(jsonPath("$.data.memo").value("개인 메모"))
                .andReturn().getResponse().getContentAsString();
        String recordId = new ObjectMapper().readTree(created).path("data").path("record_id").asText();

        mockMvc.perform(get("/treatment-records/{recordId}", recordId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duration_minutes").value(90))
                .andExpect(jsonPath("$.data.treatment_content").value("애쉬브라운 전체 염색"));
    }

    @Test
    void rejectsNegativeDurationMinutes() throws Exception {
        mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "service_types": ["CUT"],
                                  "performed_at": "2026-08-20T10:00:00Z",
                                  "duration_minutes": -1
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code")
                        .value("TREATMENT_DURATION_MINUTES_NEGATIVE"));
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

    // ------------------------------------------------------------------ 수정·삭제

    @Test
    void patchesOnlyPresentedFieldsAndClearsExplicitNulls() throws Exception {
        String recordId = createRecord(USER_ID);

        mockMvc.perform(patch("/treatment-records/" + recordId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"satisfaction":5,"memo":"  새 메모  ",
                                 "next_visit_cautions":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.record_id").value(recordId))
                .andExpect(jsonPath("$.data.salon_name").value("준헤어"))
                .andExpect(jsonPath("$.data.designer_name").value("김실장"))
                .andExpect(jsonPath("$.data.satisfaction").value(5))
                .andExpect(jsonPath("$.data.memo").value("새 메모"))
                .andExpect(jsonPath("$.data.next_visit_cautions").doesNotExist());

        var stored = jdbcTemplate.queryForMap("""
                SELECT salon_name, designer_name, satisfaction, memo, next_visit_cautions
                FROM treatment_records WHERE record_id = ?
                """, UUID.fromString(recordId));
        assertThat(stored.get("salon_name")).isEqualTo("준헤어");
        assertThat(stored.get("designer_name")).isEqualTo("김실장");
        assertThat((Number) stored.get("satisfaction")).hasToString("5");
        assertThat(stored.get("memo")).isEqualTo("새 메모");
        assertThat(stored.get("next_visit_cautions")).isNull();
    }

    @Test
    void rejectsUnknownFieldsInPatchInsteadOfSilentlyDroppingThem() throws Exception {
        UUID fileId = readyFile(USER_ID);
        String recordId = createRecord(USER_ID);

        // photos 는 이 API 가 받지 않는 필드다. 조용히 버리고 200 을 주면 클라이언트는
        // 사진이 교체된 줄 알고 옛 사진이 남은 화면을 본다.
        mockMvc.perform(patch("/treatment-records/" + recordId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memo":"메모는 정상 필드",
                                 "photos":[{"file_id":"%s","image_type":"BEFORE"}]}
                                """.formatted(fileId)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.field_errors[*].field")
                        .value(hasItem("known_fields_only")))
                .andExpect(jsonPath("$.error.field_errors[*].reason")
                        .value(hasItem(containsString("/photos"))));

        // 거절된 요청은 정상 필드까지 포함해 아무것도 반영하지 않는다.
        var stored = jdbcTemplate.queryForMap(
                "SELECT memo FROM treatment_records WHERE record_id = ?",
                UUID.fromString(recordId));
        assertThat(stored.get("memo")).isEqualTo("기존 메모");
    }

    @Test
    void rejectsFuturePerformedAtAndHidesForeignRecordDuringPatch() throws Exception {
        String ownRecordId = createRecord(USER_ID);
        mockMvc.perform(patch("/treatment-records/" + ownRecordId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"performed_at":"2099-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code")
                        .value("TREATMENT_PERFORMED_AT_IN_FUTURE"));

        String foreignRecordId = createRecord(OTHER_USER_ID);
        mockMvc.perform(patch("/treatment-records/" + foreignRecordId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"볼 수 없어야 함\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void appendsPhotoBehindExistingOnesWhenSortOrderIsOmitted() throws Exception {
        UUID firstFile = readyFile(USER_ID);
        String created = mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z", photoRefs(firstFile))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String recordId = new ObjectMapper()
                .readTree(created).path("data").path("record_id").asText();

        // sort_order 를 생략한 추가는 기존 0 번과 동점이 되면 안 된다. 동점이면 정렬이
        // created_at 으로 풀려 새 사진이 뒤로 밀리고 목록 썸네일은 옛 사진을 가리킨다.
        UUID secondFile = readyFile(USER_ID);
        mockMvc.perform(post("/treatment-records/" + recordId + "/photos")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"file_id\":\"%s\",\"image_type\":\"AFTER\"}"
                                .formatted(secondFile)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sort_order").value(1));

        mockMvc.perform(get("/treatment-records/" + recordId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.photos", hasSize(2)))
                .andExpect(jsonPath("$.data.photos[0].sort_order").value(0))
                .andExpect(jsonPath("$.data.photos[1].sort_order").value(1));
    }

    @Test
    void deletesOwnRecordAndMarksItsFilesForCleanup() throws Exception {
        UUID fileId = readyFile(USER_ID);
        String created = mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z", photoRefs(fileId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID recordId = UUID.fromString(new ObjectMapper()
                .readTree(created).path("data").path("record_id").asText());

        mockMvc.perform(delete("/treatment-records/" + recordId)
                .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM treatment_records WHERE record_id = ?",
                Integer.class, recordId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM treatment_record_photos WHERE record_id = ?",
                Integer.class, recordId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM files WHERE file_id = ?", String.class, fileId))
                .isEqualTo("DELETED");
    }

    @Test
    void hidesForeignRecordDuringDelete() throws Exception {
        String foreignRecordId = createRecord(OTHER_USER_ID);

        mockMvc.perform(delete("/treatment-records/" + foreignRecordId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM treatment_records WHERE record_id = ?",
                Integer.class, UUID.fromString(foreignRecordId))).isEqualTo(1);
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

    /**
     * READY 가 아닌 파일의 사진은 URL 을 발급하지 못한다. 이때 photo_url 은 생략되는 게 아니라
     * null 로 내려가야 한다 — 클라이언트가 "사진은 있는데 지금은 볼 수 없다"를 구분해야 한다.
     */
    @Test
    void keepsPhotoUrlNullForFilesThatAreNotReady() throws Exception {
        UUID pending = pendingFile(USER_ID);
        UUID deleted = insertFile(USER_ID, "DELETED");
        UUID recordId = insertRecordRow(USER_ID);
        insertPhotoRow(recordId, pending, "BEFORE", 0);
        insertPhotoRow(recordId, deleted, "AFTER", 1);

        mockMvc.perform(get("/treatment-records/" + recordId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.photos", hasSize(2)))
                .andExpect(jsonPath("$.data.photos[0].image_type").value("BEFORE"))
                .andExpect(jsonPath("$.data.photos[0].photo_url").hasJsonPath())
                .andExpect(jsonPath("$.data.photos[0].photo_url").value(nullValue()))
                .andExpect(jsonPath("$.data.photos[1].image_type").value("AFTER"))
                .andExpect(jsonPath("$.data.photos[1].photo_url").hasJsonPath())
                .andExpect(jsonPath("$.data.photos[1].photo_url").value(nullValue()));
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

    // ------------------------------------------------------------------ 사진 관리·비교

    @Test
    void addsUpdatesAndDeletesPhoto() throws Exception {
        String recordId = createRecord(USER_ID);
        UUID fileId = readyFile(USER_ID);
        String added = mockMvc.perform(post("/treatment-records/" + recordId + "/photos")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"file_id":"%s","image_type":"BEFORE","sort_order":2}
                                """.formatted(fileId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.record_id").value(recordId))
                .andExpect(jsonPath("$.data.image_type").value("BEFORE"))
                .andExpect(jsonPath("$.data.sort_order").value(2))
                .andExpect(jsonPath("$.data.display_url", containsString("X-Amz-Signature")))
                .andReturn().getResponse().getContentAsString();
        String photoId = new ObjectMapper().readTree(added).path("data").path("photo_id").asText();

        mockMvc.perform(patch("/treatment-records/" + recordId + "/photos/" + photoId)
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"image_type\":\"AFTER\",\"sort_order\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.image_type").value("AFTER"))
                .andExpect(jsonPath("$.data.sort_order").value(1));

        mockMvc.perform(delete("/treatment-records/" + recordId + "/photos/" + photoId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM treatment_record_photos WHERE photo_id = ?",
                Integer.class, UUID.fromString(photoId))).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM files WHERE file_id = ?", String.class, fileId))
                .isEqualTo("DELETED");
    }

    @Test
    void comparesBeforeAndAfterPhotosAndRejectsIncompletePair() throws Exception {
        UUID before = readyFile(USER_ID);
        UUID after = readyFile(USER_ID);
        String created = mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z", photoRefs(before, after))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String recordId = new ObjectMapper().readTree(created).path("data").path("record_id").asText();

        mockMvc.perform(get("/treatment-records/" + recordId + "/photo-comparison")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.record_id").value(recordId))
                .andExpect(jsonPath("$.data.before_photos", hasSize(1)))
                .andExpect(jsonPath("$.data.before_photos[0].sort_order").value(0))
                .andExpect(jsonPath("$.data.before_photos[0].display_url",
                        containsString("X-Amz-Signature")))
                .andExpect(jsonPath("$.data.after_photos", hasSize(1)))
                .andExpect(jsonPath("$.data.after_photos[0].sort_order").value(1))
                .andExpect(jsonPath("$.data.treatment_summary.service_types",
                        containsInAnyOrder("CUT", "COLOR")))
                .andExpect(jsonPath("$.data.treatment_summary.satisfaction").value(4))
                .andExpect(jsonPath("$.data.treatment_summary.next_visit_cautions")
                        .value("기존 주의사항"));

        UUID onlyBefore = readyFile(USER_ID);
        String incomplete = mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-02T10:00:00Z", photoRefs(onlyBefore))))
                .andReturn().getResponse().getContentAsString();
        String incompleteId = new ObjectMapper().readTree(incomplete)
                .path("data").path("record_id").asText();
        mockMvc.perform(get("/treatment-records/" + incompleteId + "/photo-comparison")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code")
                        .value("PHOTO_COMPARISON_NOT_AVAILABLE"));
    }

    @Test
    void enforcesTenPhotoLimitAndHidesForeignPhotoResources() throws Exception {
        UUID[] firstTen = new UUID[10];
        for (int index = 0; index < firstTen.length; index++) {
            firstTen[index] = readyFile(USER_ID);
        }
        String created = mockMvc.perform(post("/treatment-records")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("2026-08-01T10:00:00Z", photoRefs(firstTen))))
                .andReturn().getResponse().getContentAsString();
        String recordId = new ObjectMapper().readTree(created).path("data").path("record_id").asText();
        UUID eleventh = readyFile(USER_ID);

        mockMvc.perform(post("/treatment-records/" + recordId + "/photos")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"file_id":"%s","image_type":"OTHER","sort_order":10}
                                """.formatted(eleventh)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("TREATMENT_PHOTO_LIMIT_EXCEEDED"));

        mockMvc.perform(get("/treatment-records/" + recordId + "/photo-comparison")
                        .with(authentication(userAuthentication(OTHER_USER_ID))))
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

        mockMvc.perform(patch("/treatment-records/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/treatment-records/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        UUID recordId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        mockMvc.perform(post("/treatment-records/" + recordId + "/photos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/treatment-records/" + recordId + "/photos/" + photoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/treatment-records/" + recordId + "/photos/" + photoId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/treatment-records/" + recordId + "/photo-comparison"))
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
                .andExpect(jsonPath("$['paths']['/treatment-records/{recordId}']['patch']"
                        + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$['paths']['/treatment-records/{recordId}']['delete']"
                        + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$['paths']['/treatment-records/{recordId}/photos']['post']"
                        + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$['paths']['/treatment-records/{recordId}/photos/{photoId}']['patch']"
                        + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$['paths']['/treatment-records/{recordId}/photos/{photoId}']['delete']"
                        + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$['paths']['/treatment-records/{recordId}/photo-comparison']['get']"
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
                 "price_amount":120000,"price_currency":"KRW",
                 "memo":"기존 메모","next_visit_cautions":"기존 주의사항","photos":%s}
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

    private void insertAnalysisJob(UUID ownerRecordId, String status) {
        UUID photoId = readyFile(USER_ID);
        UUID recordPhotoId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_record_photos (photo_id, record_id, file_id, image_type)
                VALUES (?, ?, ?, 'AFTER')
                """, recordPhotoId, ownerRecordId, photoId);
        jdbcTemplate.update("""
                INSERT INTO analysis_jobs (
                    job_id, user_id, record_id, photo_id, status, progress, attempt_count
                ) VALUES (?, ?, ?, ?, ?, 100, 1)
                """, UUID.randomUUID(), USER_ID, ownerRecordId, recordPhotoId, status);
    }

    private void insertShare(UUID ownerId, UUID recordId, String status, Instant expiresAt) {
        UUID shareId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO shares (share_id, user_id, token_hash, status, expires_at, revoked_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, shareId, ownerId, UUID.randomUUID().toString().replace("-", ""),
                status, Timestamp.from(expiresAt),
                "REVOKED".equals(status) ? Timestamp.from(Instant.now()) : null);
        jdbcTemplate.update(
                "INSERT INTO share_records (share_id, record_id) VALUES (?, ?)",
                shareId, recordId);
        jdbcTemplate.update(
                "INSERT INTO share_fields (share_id, field_type) VALUES (?, 'PHOTOS')",
                shareId);
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

    private UUID insertRecordRow(UUID ownerId) {
        UUID recordId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO treatment_records (
                    record_id, user_id, service_types, performed_at
                ) VALUES (?, ?, ?::jsonb, now())
                """, recordId, ownerId, "[\"CUT\"]");
        return recordId;
    }

    /** 사진 순서는 created_at 순이다. now() 는 트랜잭션 시각으로 고정이라 초를 벌려 심는다. */
    private void insertPhotoRow(UUID recordId, UUID fileId, String imageType, int order) {
        jdbcTemplate.update("""
                INSERT INTO treatment_record_photos (
                    photo_id, record_id, file_id, image_type, created_at
                ) VALUES (?, ?, ?, ?, now() + (? * interval '1 second'))
                """, UUID.randomUUID(), recordId, fileId, imageType, order);
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
