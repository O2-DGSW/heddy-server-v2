package com.heddy.account;

import com.heddy.account.entity.ConsentHistory;
import com.heddy.account.entity.HairProfile;
import com.heddy.account.entity.StyleTag;
import com.heddy.account.entity.User;
import com.heddy.account.entity.UserProfile;
import com.heddy.account.entity.UserStylePreference;
import com.heddy.global.entity.BaseCreatedEntity;
import com.heddy.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 엔티티 매핑과 V1__init.sql 을 컬럼 이름 단위로 대조한다. 타입·길이·제약까지의 실제 검증은
 * 실 DB 를 쓰는 {@link AccountSchemaIntegrationTest} 가 맡고, 여기서는 그쪽이 잡지 못하는
 * 역방향(DDL 에만 있고 엔티티가 매핑하지 않는 NOT NULL 컬럼)과 상속 구조를 본다.
 */
class AccountEntityMappingTest {

    private static final Map<String, String> TABLE_BODIES =
            V1InitMigrationTest.parseTableBodies(V1InitMigrationTest.read("db/migration/V1__init.sql"));

    @ParameterizedTest
    @ValueSource(classes = {User.class, UserProfile.class, HairProfile.class,
            StyleTag.class, UserStylePreference.class, ConsentHistory.class})
    @DisplayName("엔티티의 모든 매핑 컬럼이 마이그레이션에 존재한다")
    void everyMappedColumnExistsInMigration(Class<?> entityType) {
        Table table = entityType.getAnnotation(Table.class);
        assertThat(table).as("%s 에 @Table 이 있어야 한다", entityType.getSimpleName()).isNotNull();

        String body = TABLE_BODIES.get(table.name());
        assertThat(body).as("%s 테이블이 V1 에 있어야 한다", table.name()).isNotNull();

        for (String column : mappedColumns(entityType)) {
            assertThat(body).as("%s.%s", table.name(), column).containsPattern("\\n\\s+" + column + "\\s");
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {User.class, UserProfile.class, HairProfile.class,
            StyleTag.class, UserStylePreference.class, ConsentHistory.class})
    @DisplayName("DDL 의 NOT NULL 컬럼이 전부 엔티티에 매핑돼 있다")
    void everyNotNullColumnIsMapped(Class<?> entityType) {
        String table = entityType.getAnnotation(Table.class).name();
        List<String> mapped = mappedColumns(entityType);

        Matcher matcher = Pattern.compile("\\n\\s+(\\w+)\\s+[A-Z].*?NOT NULL").matcher(TABLE_BODIES.get(table));
        while (matcher.find()) {
            assertThat(mapped).as("%s.%s 가 매핑되지 않으면 INSERT 가 런타임에 깨진다", table, matcher.group(1))
                    .contains(matcher.group(1));
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {User.class, UserProfile.class, HairProfile.class,
            StyleTag.class, UserStylePreference.class, ConsentHistory.class})
    @DisplayName("전 엔티티가 created_at 감사 상위 타입을 상속한다")
    void everyEntityExtendsBaseCreatedEntity(Class<?> entityType) {
        assertThat(BaseCreatedEntity.class).isAssignableFrom(entityType);
    }

    @ParameterizedTest
    @ValueSource(classes = {User.class, UserProfile.class, HairProfile.class,
            StyleTag.class, UserStylePreference.class})
    @DisplayName("갱신되는 엔티티는 updated_at 을 주는 BaseEntity 를 상속한다")
    void mutableEntitiesExtendBaseEntity(Class<?> entityType) {
        assertThat(BaseEntity.class).isAssignableFrom(entityType);
    }

    @Test
    @DisplayName("append-only 인 ConsentHistory 는 updated_at 을 갖지 않는다")
    void consentHistoryHasNoUpdatedAt() {
        assertThat(BaseEntity.class.isAssignableFrom(ConsentHistory.class)).isFalse();
        assertThat(mappedColumns(ConsentHistory.class)).doesNotContain("updated_at");
    }

    @Test
    @DisplayName("ConsentHistory 는 상태를 바꿀 수단을 노출하지 않는다")
    void consentHistoryExposesNoMutator() {
        assertThat(ConsentHistory.class.getMethods())
                .noneMatch(method -> method.getDeclaringClass() != Object.class
                        && method.getParameterCount() > 0
                        && !method.getName().equals("equals"));

        for (Field field : ConsentHistory.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getName().equals("id")) {
                continue;
            }
            assertThat(columnUpdatable(field)).as("%s 는 updatable = false 여야 한다", field.getName()).isFalse();
        }
    }

    private static boolean columnUpdatable(Field field) {
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null) {
            return joinColumn.updatable();
        }
        Column column = field.getAnnotation(Column.class);
        return column == null || column.updatable();
    }

    private static List<String> mappedColumns(Class<?> entityType) {
        List<String> columns = new ArrayList<>();
        for (Class<?> type = entityType; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                if (joinColumn != null) {
                    columns.add(joinColumn.name());
                    continue;
                }
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    columns.add(column.name());
                }
            }
        }
        return columns;
    }
}
