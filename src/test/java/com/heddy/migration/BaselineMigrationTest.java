package com.heddy.migration;

import com.heddy.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 베이스라인 마이그레이션이 실제 PostgreSQL 에서 적용되는지만 확인한다.
 *
 * <p>SQL 은 실행돼야 검증된다. 문자열 검사로는 문법 오류도, 제약 조건 정의 오류도 잡히지 않는다.
 * 엔티티 매핑 검증은 영속성 어댑터가 들어오는 별도 작업에서 이 베이스를 상속해 이어간다.
 */
class BaselineMigrationTest extends AbstractIntegrationTest {

    private static final List<String> BASELINE_TABLES = List.of(
            "users", "user_profiles", "hair_profiles",
            "style_tags", "user_style_preferences", "consent_history");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("V1 이 적용되면 베이스라인 테이블 6종이 생긴다")
    void baselineTablesAreCreated() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tables).containsAll(BASELINE_TABLES);
    }
}
