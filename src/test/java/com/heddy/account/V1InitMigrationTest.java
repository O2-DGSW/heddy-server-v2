package com.heddy.account;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V1__init.sql 정적 검증. 로컬 컨테이너를 띄우지 않고 방언·공통 규칙 위반을 잡는다.
 */
class V1InitMigrationTest {

    private static final String MIGRATION = read("db/migration/V1__init.sql");
    private static final Map<String, String> TABLE_BODIES = parseTableBodies(MIGRATION);

    private static final String[] TABLES = {
            "users", "user_profiles", "hair_profiles",
            "style_tags", "user_style_preferences", "consent_history"
    };

    @Test
    @DisplayName("테이블 6종을 생성한다")
    void createsSixTables() {
        assertThat(TABLE_BODIES.keySet()).containsExactlyInAnyOrder(TABLES);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DATETIME", "TINYINT", "AUTO_INCREMENT", "ENGINE=", "`"})
    @DisplayName("MySQL 문법을 쓰지 않는다")
    void usesNoMySqlSyntax(String forbidden) {
        assertThat(MIGRATION.toUpperCase(Locale.ROOT)).doesNotContain(forbidden.toUpperCase(Locale.ROOT));
    }

    @Test
    @DisplayName("소프트 삭제 컬럼을 두지 않는다")
    void hasNoSoftDeleteColumn() {
        assertThat(MIGRATION).doesNotContain("is_deleted");
    }

    @Test
    @DisplayName("이슈 #5 소유 영역(refresh_tokens)을 건드리지 않는다")
    void doesNotCreateRefreshTokens() {
        assertThat(TABLE_BODIES).doesNotContainKey("refresh_tokens");
    }

    @Test
    @DisplayName("전 테이블 PK 는 UUID 이고 DB 기본값을 쓰지 않는다")
    void everyPrimaryKeyIsApplicationGeneratedUuid() {
        for (String table : TABLES) {
            String body = TABLE_BODIES.get(table);
            assertThat(body).as("%s.id", table).containsPattern("id\\s+UUID\\s+NOT NULL");
            assertThat(body).as("%s PK 제약", table).contains("PRIMARY KEY (id)");
            assertThat(body).as("%s 는 PK 기본값을 두지 않는다", table).doesNotContain("gen_random_uuid");
        }
    }

    @Test
    @DisplayName("전 테이블에 TIMESTAMPTZ created_at 이 있다")
    void everyTableHasCreatedAt() {
        for (String table : TABLES) {
            assertThat(TABLE_BODIES.get(table)).as("%s.created_at", table)
                    .containsPattern("created_at\\s+TIMESTAMPTZ\\s+NOT NULL");
        }
    }

    @Test
    @DisplayName("users 는 email 이 식별자이고 login_id 를 두지 않는다")
    void usersAreIdentifiedByEmail() {
        String users = TABLE_BODIES.get("users");
        assertThat(users).doesNotContain("login_id");
        assertThat(users).contains("CONSTRAINT uq_users_email UNIQUE (email)");
        assertThat(users).contains("CONSTRAINT uq_users_provider_subject UNIQUE (auth_provider, provider_subject)");
        assertThat(users).containsPattern("login_fail_count\\s+SMALLINT");
        assertThat(users).containsPattern("locked_until\\s+TIMESTAMPTZ");
        assertThat(users).contains("'ACTIVE', 'LOCKED', 'DELETION_PENDING', 'DELETED'");
    }

    @Test
    @DisplayName("user_profiles 는 phone·preferred_designer·hair_cautions 를 갖는다")
    void userProfilesHaveSpecifiedColumns() {
        assertThat(TABLE_BODIES.get("user_profiles"))
                .contains("phone")
                .contains("preferred_designer")
                .contains("hair_cautions")
                .contains("UNIQUE (user_id)");
    }

    @Test
    @DisplayName("변경되는 테이블만 updated_at 을 갖는다")
    void onlyMutableTablesHaveUpdatedAt() {
        for (String table : TABLES) {
            if (table.equals("consent_history")) {
                continue;
            }
            assertThat(TABLE_BODIES.get(table)).as("%s.updated_at", table)
                    .containsPattern("updated_at\\s+TIMESTAMPTZ");
        }
    }

    @Test
    @DisplayName("consent_history 는 policy_version·source 를 갖고 append-only 다")
    void consentHistoryIsAppendOnly() {
        String consent = TABLE_BODIES.get("consent_history");
        assertThat(consent).contains("policy_version").contains("source");
        assertThat(consent).contains("agreed");
        assertThat(consent).as("append-only 테이블이라 updated_at 을 두지 않는다").doesNotContain("updated_at");
        assertThat(MIGRATION).contains("COMMENT ON TABLE consent_history IS '약관·동의 이력 (append-only)");
    }

    @Test
    @DisplayName("조회 경로마다 인덱스를 명시한다")
    void declaresIndexes() {
        assertThat(MIGRATION)
                .contains("CREATE INDEX idx_users_status")
                .contains("CREATE INDEX idx_user_style_preferences_user_type")
                .contains("CREATE INDEX idx_consent_history_user_type_created_at");
    }

    @Test
    @DisplayName("자식 테이블은 users 를 FK 로 참조한다")
    void childTablesReferenceUsers() {
        for (String table : new String[]{"user_profiles", "hair_profiles", "user_style_preferences", "consent_history"}) {
            assertThat(TABLE_BODIES.get(table)).as("%s FK", table)
                    .contains("FOREIGN KEY (user_id) REFERENCES users (id)");
        }
    }

    static String read(String classpathResource) {
        try (InputStream in = V1InitMigrationTest.class.getClassLoader().getResourceAsStream(classpathResource)) {
            assertThat(in).as("%s 가 클래스패스에 있어야 한다", classpathResource).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static Map<String, String> parseTableBodies(String sql) {
        Map<String, String> bodies = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("CREATE TABLE (\\w+) \\((.*?)\\n\\);", Pattern.DOTALL).matcher(sql);
        while (matcher.find()) {
            bodies.put(matcher.group(1), matcher.group(2));
        }
        return bodies;
    }
}
