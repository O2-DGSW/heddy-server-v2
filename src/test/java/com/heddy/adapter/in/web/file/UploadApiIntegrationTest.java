package com.heddy.adapter.in.web.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heddy.domain.file.port.in.ReclaimUploadObjectsUseCase;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
 * 발급부터 직접 업로드, 완료 검증까지 실제 프로토콜 전체를 상대로 본다. Presigned URL 은 서명이
 * 맞아야 동작하고 HEAD 는 실물 객체를 보므로, 스토리지를 가짜로 바꾸면 정작 틀릴 수 있는 부분이
 * 검증되지 않는다. S3 엔드포인트만 로컬 LocalStack 으로 돌린다.
 */
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "app.storage.bucket=heddy-test"
})
@Transactional
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UploadApiIntegrationTest extends PostgresIntegrationTest {

    private static final String BUCKET = "heddy-test";
    private static final UUID USER_ID = UUID.fromString(
            "90000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "90000000-0000-4000-8000-000000000002");
    private static final String DECLARED_SHA256 = "b".repeat(64);
    private static final byte[] UPLOAD_BYTES = "heddy".getBytes(StandardCharsets.UTF_8);

    static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer("localstack/localstack:3.8").withServices("s3");

    static {
        LOCALSTACK.start();
    }

    /**
     * 애플리케이션의 {@code S3ClientConfig} 가 만드는 클라이언트를 같은 이름의 빈으로 갈아끼운다.
     * {@link Primary} 는 혹시 두 정의가 공존하더라도 주입에서 이기기 위한 것이다.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class LocalStackStorageConfig {

        @Bean
        @Primary
        AwsCredentialsProvider awsCredentialsProvider() {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                    LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey()));
        }

        @Bean
        @Primary
        S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
            S3Client client = S3Client.builder()
                    .region(Region.of(LOCALSTACK.getRegion()))
                    .endpointOverride(LOCALSTACK.getEndpoint())
                    .credentialsProvider(credentialsProvider)
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true).build())
                    .build();
            client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
            return client;
        }

        @Bean
        @Primary
        S3Presigner s3Presigner(AwsCredentialsProvider credentialsProvider) {
            return S3Presigner.builder()
                    .region(Region.of(LOCALSTACK.getRegion()))
                    .endpointOverride(LOCALSTACK.getEndpoint())
                    .credentialsProvider(credentialsProvider)
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true).build())
                    .build();
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;
    @Autowired S3Client s3Client;
    @Autowired ReclaimUploadObjectsUseCase reclaimUploadObjectsUseCase;

    @BeforeEach
    void setUpUsers() {
        insertUser(USER_ID, "upload-user@example.com");
        insertUser(OTHER_USER_ID, "other-upload-user@example.com");
    }

    // ------------------------------------------------------------------ 정상 흐름

    @Test
    void issuesPresignedUrlUploadsDirectlyAndCompletesToReady() throws Exception {
        String responseBody = mockMvc.perform(post("/uploads/presign")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presignBody("TREATMENT_PHOTO", "image/jpeg", 5))
                        .header("X-Request-Id", "request-31"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.upload_id").isNotEmpty())
                .andExpect(jsonPath("$.data.file_id").isNotEmpty())
                .andExpect(jsonPath("$.data.method").value("PUT"))
                .andExpect(jsonPath("$.data.required_headers['If-None-Match']").value("*"))
                .andExpect(jsonPath("$.data.required_headers['Content-Type']")
                        .value("image/jpeg"))
                .andExpect(jsonPath("$.data.expires_at").isNotEmpty())
                .andExpect(jsonPath("$.request_id").value("request-31"))
                .andReturn().getResponse().getContentAsString();

        String uploadUrl = jsonField(responseBody, "upload_url");
        String uploadId = jsonField(responseBody, "upload_id");

        assertThat(put(uploadUrl, "image/jpeg", UPLOAD_BYTES)).isEqualTo(200);

        mockMvc.perform(post("/uploads/" + uploadId + "/complete")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.content_type").value("image/jpeg"))
                .andExpect(jsonPath("$.data.file_size").value(5))
                .andExpect(jsonPath("$.data.width").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.height").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.request_id").isNotEmpty());

        // READY 로 전이됐고 선언 파일명·해시가 기록된다. HEAD 로 알 수 없는 치수는 비어 있다.
        var row = jdbcTemplate.queryForMap(
                "SELECT status, file_size, file_name, sha256, width FROM files WHERE upload_id = ?",
                UUID.fromString(uploadId));
        assertThat(row.get("status")).isEqualTo("READY");
        assertThat(((Number) row.get("file_size")).longValue()).isEqualTo(5);
        assertThat(row.get("file_name")).isEqualTo("after.jpg");
        assertThat((String) row.get("sha256")).isEqualTo(DECLARED_SHA256);
        assertThat(row.get("width")).isNull();
    }

    /**
     * 재완료 규칙은 멱등 성공으로 정한다. complete 은 응답을 잃은 클라이언트가 재시도하는 요청이라,
     * 이미 READY 인 세션을 거부하면 실제로 통과한 완료 건이 실패로 기록된다. READY 는 종착 상태라
     * 저장된 메타데이터가 바뀌지 않으므로 저장된 결과를 다시 돌려주는 것이 안전하다.
     */
    @Test
    void answersRepeatedCompletionWithTheSameReadyResult() throws Exception {
        String uploadId = presignPutComplete(USER_ID);

        String firstComplete = mockMvc.perform(post("/uploads/" + uploadId + "/complete")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/uploads/" + uploadId + "/complete")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.file_id")
                        .value(jsonField(firstComplete, "file_id")))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.file_size")
                        .value(Integer.parseInt(jsonField(firstComplete, "file_size"))));
    }

    // ------------------------------------------------------------------ 사전 검증

    @Test
    void rejectsDisallowedContentTypeAtPresignTimeWithoutCreatingASession() throws Exception {
        mockMvc.perform(post("/uploads/presign")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presignBody("AR_CAPTURE", "image/heic", 1024)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("FILE_CONTENT_TYPE_NOT_ALLOWED"));

        assertThat(fileCount()).isZero();
    }

    @Test
    void rejectsDeclaredSizeOverThePurposeMaximumAtPresignTime() throws Exception {
        long overMaximum = 10L * 1024 * 1024 + 1;

        mockMvc.perform(post("/uploads/presign")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presignBody("TREATMENT_PHOTO", "image/jpeg", overMaximum)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error.code").value("FILE_TOO_LARGE"));

        assertThat(fileCount()).isZero();
    }

    /**
     * 내부 생성물 용도는 외부 발급이 거부된다. 사용자가 ANALYSIS_OVERLAY_INTERNAL 로 객체를 올릴 수
     * 있으면 이후 단계가 이 purpose 를 "시스템이 만든 파일"로 신뢰할 근거가 사라진다.
     */
    @Test
    void rejectsInternalOverlayPurposeAtPresignTimeWithoutCreatingASession() throws Exception {
        mockMvc.perform(post("/uploads/presign")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presignBody("ANALYSIS_OVERLAY_INTERNAL", "image/png", 1024)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("FILE_PURPOSE_NOT_ALLOWED"));

        assertThat(fileCount()).isZero();
    }

    @Test
    void rejectsMalformedPresignRequestsAsFieldValidation() throws Exception {
        mockMvc.perform(post("/uploads/presign")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purpose":"TREATMENT_PHOTO","content_type":"image/jpeg",
                                 "file_name":"after.jpg","file_size":0,"sha256":"%s"}
                                """.formatted(DECLARED_SHA256)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.field_errors[0].field").value("file_size"));
    }

    @Test
    void rejectsUnknownPurposeValuesAsInvalidRequest() throws Exception {
        mockMvc.perform(post("/uploads/presign")
                        .with(authentication(userAuthentication(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presignBody("NOT_A_PURPOSE", "image/jpeg", 1024)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    // ------------------------------------------------------------------ 완료 검증

    @Test
    void rejectsCompletionWhenTheObjectWasNeverUploaded() throws Exception {
        String uploadId = presignOnly(USER_ID, "TREATMENT_PHOTO", "image/jpeg");

        mockMvc.perform(post("/uploads/" + uploadId + "/complete")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FILE_OBJECT_NOT_FOUND"));
    }

    @Test
    void checksOwnershipBeforeAnythingElseSoForeignSessionsAreNotProbed() throws Exception {
        String uploadId = presignPutComplete(USER_ID);

        mockMvc.perform(post("/uploads/" + uploadId + "/complete")
                        .with(authentication(userAuthentication(OTHER_USER_ID))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_RESOURCE"));

        mockMvc.perform(post("/uploads/" + uploadId + "/complete")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsExpiredSessionsWithFileUploadExpired() throws Exception {
        String uploadId = presignOnly(USER_ID, "TREATMENT_PHOTO", "image/jpeg");
        jdbcTemplate.update(
                "UPDATE files SET expires_at = now() - interval '1 minute' WHERE upload_id = ?",
                UUID.fromString(uploadId));
        // 벌크 UPDATE 는 영속성 컨텍스트를 거치지 않으므로 비워 다음 읽기가 DB 를 보게 한다.
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post("/uploads/" + uploadId + "/complete")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("FILE_UPLOAD_EXPIRED"));
    }

    @Test
    void rejectsObjectsWhoseStoredContentTypeDiffersFromTheSession() throws Exception {
        String responseBody = presign(USER_ID, "TREATMENT_PHOTO", "image/jpeg", 5);
        String uploadUrl = jsonField(responseBody, "upload_url");
        String uploadId = jsonField(responseBody, "upload_id");

        // 서명된 Content-Type 과 다르게 올린다. LocalStack 은 서명 헤더를 강제하지 않으므로
        // 객체가 png 로 기록되고, 불일치 판정은 complete 의 HEAD 대조가 맡는다.
        assertThat(put(uploadUrl, "image/png", UPLOAD_BYTES)).isEqualTo(200);

        mockMvc.perform(post("/uploads/" + uploadId + "/complete")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("FILE_CONTENT_TYPE_MISMATCH"));
    }

    /**
     * 명세의 "크기 일치" 검증. presigned PUT 은 크기를 서명하지 않아 선언과 다른 크기가 올라갈 수
     * 있고, purpose 최대치 이하라 해도 READY 로 통과시키면 안 된다.
     */
    @Test
    void rejectsObjectsWhoseActualSizeDiffersFromTheDeclaration() throws Exception {
        String responseBody = presign(USER_ID, "TREATMENT_PHOTO", "image/jpeg", 5);
        String uploadUrl = jsonField(responseBody, "upload_url");
        String uploadId = jsonField(responseBody, "upload_id");

        byte[] differentSize = "heddy!".getBytes(StandardCharsets.UTF_8);
        assertThat(put(uploadUrl, "image/jpeg", differentSize)).isEqualTo(200);

        mockMvc.perform(post("/uploads/" + uploadId + "/complete")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("FILE_SIZE_MISMATCH"));

        assertThat(statusOf(uploadId)).isEqualTo("PENDING");
    }

    /**
     * 완료 뒤 같은 URL 로 덮어쓰면 READY 내용이 몰래 바뀐다. 발급 때 서명한 If-None-Match:* 조건이
     * 이미 존재하는 객체에 대한 두 번째 PUT 을 스토리지에서 막는지 프로토콜 전체로 확인한다.
     */
    @Test
    void blocksOverwriteThroughTheSameUrlAfterCompletion() throws Exception {
        String responseBody = presign(USER_ID, "TREATMENT_PHOTO", "image/jpeg", 5);
        String uploadUrl = jsonField(responseBody, "upload_url");
        String uploadId = jsonField(responseBody, "upload_id");

        assertThat(put(uploadUrl, "image/jpeg", UPLOAD_BYTES)).isEqualTo(200);
        mockMvc.perform(post("/uploads/" + uploadId + "/complete")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk());

        byte[] replacement = "stolen".getBytes(StandardCharsets.UTF_8);
        assertThat(put(uploadUrl, "image/jpeg", replacement)).isEqualTo(412);

        mockMvc.perform(post("/uploads/" + uploadId + "/complete")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.file_size").value(5));
    }

    /**
     * 최대치 초과도 결국 선언 불일치다. 일치 검증이 먼저 막고, 도메인 문턱(TOO_LARGE)은
     * 선언과 무관하게 객체가 직접 쓰이는 경로를 위한 최후 방어로 남는다.
     */
    @Test
    void reportsSizeMismatchEvenWhenActualObjectExceedsThePurposeMaximum()
            throws Exception {
        long withinMaximum = 1024;
        int actualOverMaximum = 5 * 1024 * 1024 + 1;
        String responseBody = presign(USER_ID, "AR_CAPTURE", "image/jpeg", withinMaximum);
        String uploadUrl = jsonField(responseBody, "upload_url");
        String uploadId = jsonField(responseBody, "upload_id");

        assertThat(put(uploadUrl, "image/jpeg", new byte[actualOverMaximum])).isEqualTo(200);

        mockMvc.perform(post("/uploads/" + uploadId + "/complete")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("FILE_SIZE_MISMATCH"));

        assertThat(statusOf(uploadId)).isEqualTo("PENDING");
    }

    @Test
    void reportsUnknownUploadIdsAsResourceNotFound() throws Exception {
        mockMvc.perform(post("/uploads/" + UUID.randomUUID() + "/complete")
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void requiresAuthenticationForEveryUploadEndpoint() throws Exception {
        mockMvc.perform(post("/uploads/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presignBody("TREATMENT_PHOTO", "image/jpeg", 1024)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/uploads/" + UUID.randomUUID() + "/complete"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/uploads/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ 취소

    /** 취소는 스토리지 객체와 세션 행을 함께 정리한다. 실물이 사라졌는지는 HEAD 로 직접 본다. */
    @Test
    void cancelsAPendingSessionAndRemovesItsStoredObject() throws Exception {
        String responseBody = presign(USER_ID, "TREATMENT_PHOTO", "image/jpeg", UPLOAD_BYTES.length);
        String uploadUrl = jsonField(responseBody, "upload_url");
        String uploadId = jsonField(responseBody, "upload_id");
        assertThat(put(uploadUrl, "image/jpeg", UPLOAD_BYTES)).isEqualTo(200);

        mockMvc.perform(delete("/uploads/" + uploadId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNoContent());

        assertThat(objectAbsent(objectKeyOf(uploadId))).isTrue();
        assertThat(statusOf(uploadId)).isEqualTo("DELETED");
    }

    /**
     * 한 번도 업로드되지 않은 세션의 취소와 그 재요청. 지울 객체가 없어도 S3 는 성공이고,
     * DELETED 뒤의 재요청은 멱등하게 다시 204 다.
     */
    @Test
    void answersCancellationOfANeverUploadedSessionAndItsRepeatWithNoContent()
            throws Exception {
        String uploadId = presignOnly(USER_ID, "TREATMENT_PHOTO", "image/jpeg");

        mockMvc.perform(delete("/uploads/" + uploadId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/uploads/" + uploadId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNoContent());

        assertThat(statusOf(uploadId)).isEqualTo("DELETED");
    }

    @Test
    void rejectsForeignCancellationWithForbiddenResource() throws Exception {
        String uploadId = presignOnly(USER_ID, "TREATMENT_PHOTO", "image/jpeg");

        mockMvc.perform(delete("/uploads/" + uploadId)
                        .with(authentication(userAuthentication(OTHER_USER_ID))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_RESOURCE"));

        assertThat(statusOf(uploadId)).isEqualTo("PENDING");
    }

    @Test
    void reportsUnknownUploadIdsAsResourceNotFoundOnCancellation() throws Exception {
        mockMvc.perform(delete("/uploads/" + UUID.randomUUID())
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    /** READY 는 다른 도메인이 참조할 수 있는 검증된 파일이라 업로드 취소 대상이 아니다. */
    @Test
    void rejectsCancellingAReadySessionWithInvalidState() throws Exception {
        String uploadId = presignPutComplete(USER_ID);

        mockMvc.perform(delete("/uploads/" + uploadId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("FILE_INVALID_STATE"));

        assertThat(statusOf(uploadId)).isEqualTo("READY");
    }

    /**
     * 취소 뒤에도 이미 발급된 presigned PUT URL 은 세션 만료까지 유효하다. 전송 중이던 PUT 이나
     * 클라이언트 재시도가 도착하면 객체만 되살아나고, 행은 이미 DELETED 라 PENDING·READY 를 훑는
     * 정리 작업이 잡지 못한다. 만료 이후 회수 경로가 같은 키를 다시 지우는지 실물로 확인한다.
     */
    @Test
    void reclaimsObjectsResurrectedByTheStillValidUploadUrlAfterCancellation() throws Exception {
        String responseBody = presign(USER_ID, "TREATMENT_PHOTO", "image/jpeg", UPLOAD_BYTES.length);
        String uploadUrl = jsonField(responseBody, "upload_url");
        String uploadId = jsonField(responseBody, "upload_id");
        assertThat(put(uploadUrl, "image/jpeg", UPLOAD_BYTES)).isEqualTo(200);
        mockMvc.perform(delete("/uploads/" + uploadId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNoContent());

        // 취소가 지운 자리에 같은 URL 로 객체가 다시 생긴다. If-None-Match: * 도 막지 못한다.
        assertThat(put(uploadUrl, "image/jpeg", UPLOAD_BYTES)).isEqualTo(200);
        assertThat(objectAbsent(objectKeyOf(uploadId))).isFalse();

        expireSession(uploadId);
        assertThat(reclaimUploadObjectsUseCase.reclaimExpired(10)).isEqualTo(1);

        assertThat(objectAbsent(objectKeyOf(uploadId))).isTrue();
        assertThat(reclaimedAtOf(uploadId)).isNotNull();
        assertThat(statusOf(uploadId)).isEqualTo("DELETED");
    }

    /** 회수가 끝난 세션은 다음 회차의 대상이 아니다. */
    @Test
    void doesNotReclaimTheSameCancelledSessionTwice() throws Exception {
        String uploadId = presignOnly(USER_ID, "TREATMENT_PHOTO", "image/jpeg");
        mockMvc.perform(delete("/uploads/" + uploadId)
                        .with(authentication(userAuthentication(USER_ID))))
                .andExpect(status().isNoContent());
        expireSession(uploadId);

        assertThat(reclaimUploadObjectsUseCase.reclaimExpired(10)).isEqualTo(1);
        assertThat(reclaimUploadObjectsUseCase.reclaimExpired(10)).isZero();
    }

    // ------------------------------------------------------------------ 문서화

    @Test
    void documentsUploadEndpointsAndConstraintsInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/uploads/presign']['post']"
                        + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$['paths']['/uploads/{uploadId}/complete']['post']"
                        + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$['paths']['/uploads/{uploadId}']['delete']"
                        + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$.components.schemas.PresignUploadRequest"
                        + ".properties.purpose.description").value(containsString("TREATMENT_PHOTO")))
                .andExpect(jsonPath("$.components.schemas.PresignUploadRequest"
                        + ".properties.file_size.description")
                        .value(containsString("재검증")))
                .andExpect(jsonPath("$.components.schemas.PresignUploadRequest"
                        + ".properties.file_name.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.PresignUploadRequest.required",
                        org.hamcrest.Matchers.hasItem("file_name")))
                .andExpect(jsonPath("$.components.schemas.PresignUploadRequest"
                        + ".properties.sha256.pattern").value("^[0-9a-fA-F]{64}$"))
                .andExpect(jsonPath("$.components.schemas.PresignUploadResponse"
                        + ".properties.method.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.PresignUploadResponse"
                        + ".properties.required_headers.description").isNotEmpty())
                .andExpect(jsonPath("$.components.schemas.CompleteUploadResponse"
                        + ".properties.width").exists())
                .andExpect(jsonPath("$.components.schemas.CompleteUploadResponse"
                        + ".properties.height").exists());
    }

    // ------------------------------------------------------------------ 헬퍼

    /** presign 만 하고 끝낸다. upload_id 를 돌려준다. */
    private String presignOnly(UUID userId, String purpose, String contentType) throws Exception {
        String responseBody = presign(userId, purpose, contentType, UPLOAD_BYTES.length);
        return jsonField(responseBody, "upload_id");
    }

    private String presign(UUID userId, String purpose, String contentType, long fileSize)
            throws Exception {
        return mockMvc.perform(post("/uploads/presign")
                        .with(authentication(userAuthentication(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presignBody(purpose, contentType, fileSize)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String presignBody(String purpose, String contentType, long fileSize) {
        return """
                {"purpose":"%s","content_type":"%s","file_name":"after.jpg",
                 "file_size":%d,"sha256":"%s"}
                """.formatted(purpose, contentType, fileSize, DECLARED_SHA256);
    }

    /** presign → PUT → complete 까지 정상으로 밟고 upload_id 를 돌려준다. */
    private String presignPutComplete(UUID userId) throws Exception {
        String responseBody = presign(userId, "TREATMENT_PHOTO", "image/jpeg", UPLOAD_BYTES.length);
        String uploadUrl = jsonField(responseBody, "upload_url");
        String uploadId = jsonField(responseBody, "upload_id");
        assertThat(put(uploadUrl, "image/jpeg", UPLOAD_BYTES)).isEqualTo(200);
        mockMvc.perform(post("/uploads/" + uploadId + "/complete")
                        .with(authentication(userAuthentication(userId))))
                .andExpect(status().isOk());
        return uploadId;
    }

    private static String jsonField(String responseBody, String field) {
        try {
            return new ObjectMapper()
                    .readTree(responseBody).path("data").path(field).asText();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static int put(String url, String contentType, byte[] body)
            throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", contentType)
                        // 발급 응답의 required_headers 그대로다. 조건이 서명에 포함돼 있다.
                        .header("If-None-Match", "*")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    /** 발급된 업로드 URL 이 만료된 시점을 만든다. 회수는 그 뒤에야 의미가 있다. */
    private void expireSession(String uploadId) {
        jdbcTemplate.update(
                "UPDATE files SET expires_at = now() - interval '1 minute' WHERE upload_id = ?",
                UUID.fromString(uploadId));
    }

    private java.time.Instant reclaimedAtOf(String uploadId) {
        return jdbcTemplate.queryForObject(
                "SELECT reclaimed_at FROM files WHERE upload_id = ?", java.time.Instant.class,
                UUID.fromString(uploadId));
    }

    private String statusOf(String uploadId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM files WHERE upload_id = ?", String.class,
                UUID.fromString(uploadId));
    }

    private String objectKeyOf(String uploadId) {
        return jdbcTemplate.queryForObject(
                "SELECT object_key FROM files WHERE upload_id = ?", String.class,
                UUID.fromString(uploadId));
    }

    /** 객체 키가 스토리지에 실제로 없는지 HEAD 로 확인한다. 취소의 핵심 결과라 가짜로 안 본다. */
    private boolean objectAbsent(String objectKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(BUCKET).key(objectKey).build());
            return false;
        } catch (NoSuchKeyException exception) {
            return true;
        }
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

    private int fileCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM files", Integer.class);
        return count == null ? 0 : count;
    }
}
