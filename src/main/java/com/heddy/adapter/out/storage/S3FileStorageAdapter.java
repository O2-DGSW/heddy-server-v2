package com.heddy.adapter.out.storage;

import com.heddy.domain.file.model.StorageObject;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileStoragePort;
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
import java.util.Optional;

@Component
public class S3FileStorageAdapter implements FileStoragePort {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration uploadUrlTtl;
    private final Duration downloadUrlTtl;

    S3FileStorageAdapter(
            S3Client s3Client,
            S3Presigner presigner,
            @Value("${app.storage.bucket}") String bucket,
            @Value("${app.storage.upload-url-ttl-seconds}") long uploadUrlTtlSeconds,
            @Value("${app.storage.download-url-ttl-seconds}") long downloadUrlTtlSeconds
    ) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.bucket = bucket;
        this.uploadUrlTtl = Duration.ofSeconds(uploadUrlTtlSeconds);
        this.downloadUrlTtl = Duration.ofSeconds(downloadUrlTtlSeconds);
    }

    /**
     * Content-Type 을 서명에 포함한다. 그래야 클라이언트가 다른 형식으로 바꿔 올릴 때 S3 가 거부한다.
     * 크기는 서명에 넣지 않는다 — 넣으면 바이트 단위까지 정확히 맞아야 해서 EXIF 처리나 인코더
     * 차이로 몇 바이트만 달라져도 업로드가 실패한다. 크기 검증은 완료 시점의 HEAD 로 한다.
     */
    @Override
    public URI createUploadUrl(StoredFile file) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(file.objectKey())
                .contentType(file.contentType())
                .build();
        return URI.create(presigner.presignPutObject(PutObjectPresignRequest.builder()
                        .signatureDuration(uploadUrlTtl)
                        .putObjectRequest(putRequest)
                        .build())
                .url()
                .toString());
    }

    @Override
    public URI createDownloadUrl(StoredFile file) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(file.objectKey())
                .build();
        return URI.create(presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(downloadUrlTtl)
                        .getObjectRequest(getRequest)
                        .build())
                .url()
                .toString());
    }

    @Override
    public Optional<StorageObject> findObject(String objectKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
            return Optional.of(new StorageObject(response.contentType(), response.contentLength()));
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (S3Exception exception) {
            // 404 를 NoSuchKeyException 으로 올려주지 않는 구현이 있다(HeadObject 는 본문이 없어
            // 오류 코드를 실을 자리가 없다). 상태 코드로 한 번 더 걸러낸다.
            if (exception.statusCode() == 404) {
                return Optional.empty();
            }
            throw exception;
        }
    }
}
