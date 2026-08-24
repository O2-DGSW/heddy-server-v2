package com.heddy.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 스프링 컨텍스트를 띄우는 테스트의 공용 베이스. 운영과 같은 PostgreSQL 16 을 상대로 돌린다.
 *
 * <p>H2 는 {@code MODE=PostgreSQL} 이어도 {@code CHAR} 와 {@code VARCHAR} 를 구분하지 않아
 * {@code ddl-auto: validate} 가 잡아야 할 타입 불일치를 통과시킨다. 앞으로 들어올 JSONB·GIN 인덱스,
 * 부분 유니크 인덱스, TIMESTAMPTZ 정밀도도 마찬가지로 H2 에서는 검증되지 않는다.
 *
 * <p>컨테이너는 static 으로 한 번만 띄우고 모든 하위 테스트가 공유한다. 종료는 Testcontainers 의
 * Ryuk 에 맡긴다. 테스트마다 새로 띄우면 실행 시간이 컨테이너 기동 시간만큼 곱해진다.
 */
@ActiveProfiles("test")
@SpringBootTest
public abstract class PostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
