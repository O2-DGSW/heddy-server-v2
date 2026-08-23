package com.heddy.adapter.out.storage;

import com.heddy.domain.file.model.FilePurpose;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 S3 프로토콜을 상대로 검증한다. Presigned URL 은 서명 계산이 맞아야 동작하므로
 * 가짜 구현으로 바꿔치기하면 정작 틀릴 수 있는 부분이 검증되지 않는다.
 */
class S3FileStorageAdapterIntegrationTest {

    private static final String BUCKET = "heddy-test";
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final byte[] CONTENT = "heddy".getBytes(StandardCharsets.UTF_8);

    static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer("localstack/localstack:3.8").withServices("s3");

    private static S3FileStorageAdapter adapter;
    private static S3Client s3Client;

    @BeforeAll
    static void startStorage() {
        LOCALSTACK.start();
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(
                LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey()));
        var pathStyle = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        s3Client = S3Client.builder()
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

        adapter = new S3FileStorageAdapter(s3Client, presigner, BUCKET, 300, 60);
    }

    @Test
    void uploadsThroughPresignedUrlWithoutPassingBytesThroughTheServer() throws Exception {
        StoredFile file = pendingPhoto();

        int status = put(adapter.createUploadUrl(file), file.contentType(), CONTENT);

        assertThat(status).isEqualTo(200);
        assertThat(adapter.findObject(file.objectKey())).isPresent();
    }

    /**
     * Content-Type 이 서명에 포함돼야 클라이언트가 다른 형식으로 바꿔 올릴 때 S3 가 거부한다.
     *
     * <p>거부 자체를 확인하지 않고 서명 대상만 확인하는 이유는, LocalStack 이 서명을 검증하지 않아
     * 잘못된 Content-Type 으로 올려도 200 을 주기 때문이다. 실제 거부 동작은 개발용 S3 에서 확인한다.
     */
    @Test
    void signsContentTypeAndExpirySoStorageCanRejectMismatchedUploads() {
        URI url = adapter.createUploadUrl(pendingPhoto());

        assertThat(url.getQuery()).contains("X-Amz-SignedHeaders=content-type;host");
        assertThat(url.getQuery()).contains("X-Amz-Expires=300");
    }

    @Test
    void givesDownloadUrlAShorterLifetimeThanUpload() {
        URI url = adapter.createDownloadUrl(pendingPhoto());

        assertThat(url.getQuery()).contains("X-Amz-Expires=60");
    }

    @Test
    void reportsActualContentTypeAndSizeFromStorage() throws Exception {
        StoredFile file = pendingPhoto();
        put(adapter.createUploadUrl(file), file.contentType(), CONTENT);

        StorageObject object = adapter.findObject(file.objectKey()).orElseThrow();

        assertThat(object.contentType()).isEqualTo("image/jpeg");
        assertThat(object.byteSize()).isEqualTo(CONTENT.length);
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

    private static StoredFile pendingPhoto() {
        String objectKey = ObjectKeyGenerator.generate(
                FilePurpose.TREATMENT_PHOTO, OWNER_ID, "image/jpeg");
        return StoredFile.pending(
                OWNER_ID, FilePurpose.TREATMENT_PHOTO, objectKey, "image/jpeg", CONTENT.length);
    }

    private static int put(URI url, String contentType, byte[] body) throws IOException, InterruptedException {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(url)
                        .header("Content-Type", contentType)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()).statusCode();
    }
}
