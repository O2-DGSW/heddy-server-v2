package com.heddy.adapter.out.storage;

import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.PresignedUpload;
import com.heddy.domain.file.model.StorageObject;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.service.ObjectKeyGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 S3 프로토콜을 상대로 검증한다. Presigned URL 은 서명 계산이 맞아야 동작하므로
 * 가짜 구현으로 바꿔치기하면 정작 틀릴 수 있는 부분이 검증되지 않는다.
 */
class S3FileStorageAdapterIntegrationTest {

    private static final String BUCKET = "heddy-test";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final byte[] CONTENT = "heddy".getBytes(StandardCharsets.UTF_8);
    private static final DateTimeFormatter AMZ_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC);

    static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer("localstack/localstack:3.8").withServices("s3");

    private static S3FileStorageAdapter adapter;

    @BeforeAll
    static void startStorage() {
        LOCALSTACK.start();
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(
                LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey()));
        var pathStyle = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        S3Client s3Client = S3Client.builder()
                .region(Region.of(LOCALSTACK.getRegion()))
                .endpointOverride(LOCALSTACK.getEndpoint())
                .credentialsProvider(credentials)
                .serviceConfiguration(pathStyle)
                .build();
        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());

        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(LOCALSTACK.getRegion()))
                .endpointOverride(LOCALSTACK.getEndpoint())
                .credentialsProvider(credentials)
                .serviceConfiguration(pathStyle)
                .build();

        adapter = new S3FileStorageAdapter(s3Client, presigner, BUCKET, 60);
    }

    // ------------------------------------------------------------------ 업로드·조회

    @Test
    void uploadsThroughPresignedUrlWithoutPassingBytesThroughTheServer() throws Exception {
        StoredFile file = pendingPhoto();

        int status = put(adapter.createUploadUrl(file), file.contentType(), CONTENT);

        assertThat(status).isEqualTo(200);
        assertThat(adapter.findObject(file.objectKey())).isPresent();
    }

    @Test
    void reportsActualContentTypeAndSizeFromStorage() throws Exception {
        StoredFile file = pendingPhoto();
        put(adapter.createUploadUrl(file), file.contentType(), CONTENT);

        StorageObject object = adapter.findObject(file.objectKey()).orElseThrow();

        assertThat(object.contentType()).isEqualTo("image/jpeg");
        assertThat(object.byteSize()).isEqualTo(CONTENT.length);
    }

    /**
     * 덮어쓰기 방지 조건이 실제로 스토리지까지 적용되는지 본다. 같은 URL 로 두 번째 PUT 은
     * 대상이 이미 존재하므로 412 로 막혀야 한다. READY 로 확정된 객체가 완료 뒤 몰래 바뀌는
     * 경로를 이 조건이 닫는다.
     */
    @Test
    void refusesToOverwriteAnExistingObjectThroughTheSameSignedUrl() throws Exception {
        StoredFile file = pendingPhoto();
        PresignedUpload upload = adapter.createUploadUrl(file);

        assertThat(put(upload, file.contentType(), CONTENT)).isEqualTo(200);
        byte[] replacement = "different-content".getBytes(StandardCharsets.UTF_8);
        assertThat(put(upload, file.contentType(), replacement)).isEqualTo(412);

        assertThat(adapter.findObject(file.objectKey()).orElseThrow().byteSize())
                .isEqualTo(CONTENT.length);
    }

    @Test
    void reportsNothingForKeyThatWasNeverUploaded() {
        Optional<StorageObject> object = adapter.findObject("TREATMENT_PHOTO/nobody/missing.jpg");

        assertThat(object).isEmpty();
    }

    @Test
    void servesUploadedObjectThroughPresignedDownloadUrl() throws Exception {
        StoredFile file = pendingPhoto();
        put(adapter.createUploadUrl(file), file.contentType(), CONTENT);

        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(adapter.createDownloadUrl(file)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(CONTENT);
    }

    // ------------------------------------------------------------------ 만료

    /**
     * URL 만료와 업로드 세션 만료가 같은 시각이어야 한다. 어긋나면 서버는 만료로 거부하는데
     * URL 은 아직 살아 있거나, API 가 안내한 시각보다 URL 이 먼저 죽는 구간이 생긴다.
     */
    @Test
    void signsUploadUrlUntilTheUploadSessionExpires() {
        StoredFile file = pendingPhoto();

        Instant urlExpiry = expiryOf(adapter.createUploadUrl(file).url());

        assertThat(Duration.between(urlExpiry, file.expiresAt()).abs())
                .isLessThanOrEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void refusesToIssueUploadUrlForAnAlreadyExpiredSession() {
        StoredFile expired = StoredFile.pending(
                USER_ID, FilePurpose.TREATMENT_PHOTO, objectKey(), "image/jpeg", "expired.jpg",
                CONTENT.length, "b".repeat(64), Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> adapter.createUploadUrl(expired))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givesDownloadUrlItsOwnShortLifetime() {
        URI url = adapter.createDownloadUrl(pendingPhoto());

        assertThat(queryOf(url).get("X-Amz-Expires")).isEqualTo("60");
    }

    /**
     * Content-Type 과 덮어쓰기 방지 조건이 모두 서명 대상이어야 한다. Content-Type 이 빠지면
     * 클라이언트가 형식을 바꿔 올릴 수 있고, If-None-Match 가 빠지면 조건 없는 PUT 으로 READY
     * 객체를 덮어쓸 수 있다.
     *
     * <p>거부 동작 자체는 {@link #refusesToOverwriteAnExistingObjectThroughTheSameSignedUrl} 이
     * 확인한다. 여기서 서명 대상만 따로 보는 이유는, LocalStack 이 서명 검증은 느슨해도 조건
     * 적용은 하므로 둘의 근거를 분리해야 어디가 깨졌는지 알아보기 쉽기 때문이다.
     */
    @Test
    void signsContentTypeAndWriteConditionSoStorageCanRejectUnsafeUploads() {
        PresignedUpload upload = adapter.createUploadUrl(pendingPhoto());

        assertThat(queryOf(upload.url()).get("X-Amz-SignedHeaders"))
                .isEqualTo("content-type;host;if-none-match");
        assertThat(upload.method()).isEqualTo("PUT");
        assertThat(upload.requiredHeaders())
                .containsEntry("Content-Type", "image/jpeg")
                .containsEntry("If-None-Match", "*");
    }

    // ------------------------------------------------------------------ 헬퍼

    private static Instant expiryOf(URI url) {
        Map<String, String> query = queryOf(url);
        Instant signedAt = Instant.from(AMZ_DATE.parse(query.get("X-Amz-Date")));
        return signedAt.plus(Duration.ofSeconds(Long.parseLong(query.get("X-Amz-Expires"))));
    }

    private static Map<String, String> queryOf(URI url) {
        return java.util.Arrays.stream(url.getQuery().split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(pair -> pair[0], pair -> pair.length > 1 ? pair[1] : ""));
    }

    private static String objectKey() {
        return ObjectKeyGenerator.generate(FilePurpose.TREATMENT_PHOTO, USER_ID, "image/jpeg");
    }

    private static StoredFile pendingPhoto() {
        return StoredFile.pending(
                USER_ID, FilePurpose.TREATMENT_PHOTO, objectKey(), "image/jpeg", "after.jpg",
                CONTENT.length, "b".repeat(64), Instant.now().plus(5, ChronoUnit.MINUTES));
    }

    private static int put(PresignedUpload upload, String contentType, byte[] body)
            throws IOException, InterruptedException {
        return put(upload.url(), contentType, body);
    }

    private static int put(URI url, String contentType, byte[] body)
            throws IOException, InterruptedException {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(url)
                        .header("Content-Type", contentType)
                        // 발급 응답의 required_headers 가 클라이언트가 보내야 하는 그대로다.
                        .header("If-None-Match", "*")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()).statusCode();
    }
}
