package com.heddy.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 PostgreSQL 16 컨테이너 위에서 도는 통합 테스트의 공통 베이스.
 *
 * <p>컨테이너는 {@code static} 이라 JVM 당 한 번만 뜨고 하위 테스트 전체가 공유한다.
 * {@link ServiceConnection} 이 데이터소스 속성을 컨테이너에 자동으로 연결하므로
 * {@code integration} 프로파일은 Flyway 를 켜는 일만 한다.
 *
 * <p>스키마는 Flyway 가 {@code classpath:db/migration} 을 적용해 만든다.
 * 즉 이 베이스를 상속한 테스트는 운영과 같은 경로로 만들어진 스키마를 본다.
 */
@ActiveProfiles("integration")
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
}
