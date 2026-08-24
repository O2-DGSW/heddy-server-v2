package com.heddy.adapter.out.storage;

import com.heddy.domain.file.model.StorageObject;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class S3FileStorageAdapter implements FileStoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorageAdapter.class);

    /** Content-Type 없이 올라온 객체. 허용 목록에 없으므로 완료 검증에서 걸린다. */
    private static final String UNKNOWN_CONTENT_TYPE = "application/octet-stream";

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration downloadUrlTtl;

    S3FileStorageAdapter(
            S3Client s3Client,
            S3Presigner presigner,
            @Value("${app.storage.bucket}") String bucket,
            @Value("${app.storage.download-url-ttl-seconds}") long downloadUrlTtlSeconds
    ) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.bucket = bucket;
        this.downloadUrlTtl = Duration.ofSeconds(downloadUrlTtlSeconds);
    }

    /**
     * 업로드 세션이 끝나는 시각까지만 유효한 PUT URL 을 발급한다.
     *
     * <p>설정값 TTL 로 따로 서명하지 않는다. 그러면 URL 만료와 DB 의 {@code expires_at} 이 서로 다른
     * 시각이 되어, 서버는 만료로 거부하는데 URL 은 아직 살아 있거나 그 반대인 구간이 생긴다.
     *
     * <p>Content-Type 을 서명에 포함한다. 그래야 클라이언트가 다른 형식으로 바꿔 올릴 때 S3 가 거부한다.
     * 크기는 서명에 넣지 않는다 — 넣으면 바이트 단위까지 정확히 맞아야 해서 EXIF 처리나 인코더 차이로
     * 몇 바이트만 달라져도 업로드가 실패한다. 크기 검증은 완료 시점의 HEAD 로 한다.
     */
    @Override
    public URI createUploadUrl(StoredFile file) {
        Duration untilSessionExpiry = Duration.between(Instant.now(), file.expiresAt());
        if (untilSessionExpiry.isNegative() || untilSessionExpiry.isZero()) {
            throw new IllegalStateException("이미 만료된 업로드 세션에는 URL 을 발급할 수 없습니다");
        }
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(file.objectKey())
                .contentType(file.contentType())
                .build();
        return toUri(presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(untilSessionExpiry)
                .putObjectRequest(putRequest)
                .build()).url());
    }

    @Override
    public URI createDownloadUrl(StoredFile file) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(file.objectKey())
                .build();
        return toUri(presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(downloadUrlTtl)
                .getObjectRequest(getRequest)
                .build()).url());
    }

    @Override
    public Optional<StorageObject> findObject(String objectKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
            return Optional.of(new StorageObject(
                    response.contentType() == null ? UNKNOWN_CONTENT_TYPE : response.contentType(),
                    response.contentLength() == null ? 0L : response.contentLength()));
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (S3Exception exception) {
            return emptyIfObjectAbsent(objectKey, exception);
        }
    }

    /**
     * HEAD 는 응답 본문이 없어 오류 코드를 실을 자리가 없다. 상태 코드로만 판단해야 한다.
     *
     * <p>403 을 "없음"으로 보는 이유는, 버킷에 {@code s3:ListBucket} 권한이 없으면 S3 가 존재하지 않는
     * 키에 404 대신 403 을 주기 때문이다. 흔한 IAM 구성이라 403 을 오류로 올리면 업로드가 실패한
     * 정상 흐름이 전부 500 이 된다. 다만 권한 자체가 잘못된 경우와 구분되지 않으므로 경고를 남긴다.
     * {@code s3:ListBucket} 을 부여하면 404 가 정확해진다.
     *
     * <p>그 밖의 오류는 삼키지 않는다. 특히 버킷이 없을 때 조용히 "없음"을 돌려주면 스토리지 설정
     * 오류가 파일 단위의 정상적인 부재처럼 보인다.
     */
    private Optional<StorageObject> emptyIfObjectAbsent(String objectKey, S3Exception exception) {
        int status = exception.statusCode();
        if (status == 404) {
            return Optional.empty();
        }
        if (status == 403) {
            log.warn("S3 HEAD 403. 객체가 없거나 s3:ListBucket 권한이 없습니다. key={}", objectKey);
            return Optional.empty();
        }
        throw exception;
    }

    private static URI toUri(java.net.URL url) {
        return URI.create(url.toString());
    }
}
