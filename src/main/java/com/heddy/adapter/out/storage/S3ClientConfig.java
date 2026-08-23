package com.heddy.adapter.out.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3 클라이언트 구성. {@code app.storage.endpoint} 가 비어 있으면 실제 AWS 로, 채워져 있으면
 * 그 주소(로컬 MinIO)로 붙는다.
 *
 * <p>엔드포인트를 지정할 때는 path-style 접근을 켠다. {@code bucket.host} 형태의 가상 호스트
 * 주소는 로컬 컨테이너에서 이름 해석이 되지 않는다.
 */
@Configuration
class S3ClientConfig {

    private final String region;
    private final String endpoint;
    private final String accessKey;
    private final String secretKey;

    S3ClientConfig(
            @Value("${app.storage.region}") String region,
            @Value("${app.storage.endpoint:}") String endpoint,
            @Value("${app.storage.access-key:}") String accessKey,
            @Value("${app.storage.secret-key:}") String secretKey
    ) {
        this.region = region;
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Bean
    S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(pathStyleAccess());
        }
        return builder.build();
    }

    @Bean
    S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(pathStyleAccess());
        }
        return builder.build();
    }

    private S3Configuration pathStyleAccess() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }

    /** 로컬·개발 환경은 정적 키를, 운영은 인스턴스 역할 등 기본 체인을 쓴다. */
    private AwsCredentialsProvider credentialsProvider() {
        if (accessKey.isBlank()) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
}
