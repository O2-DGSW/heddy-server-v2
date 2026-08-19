package com.heddy.account;

import com.heddy.account.entity.ConsentHistory;
import com.heddy.account.entity.HairProfile;
import com.heddy.account.entity.StyleTag;
import com.heddy.account.entity.User;
import com.heddy.account.entity.UserProfile;
import com.heddy.account.entity.UserStylePreference;
import com.heddy.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 엔티티 매핑이 V1__init.sql 과 어긋나지 않는지 검증한다.
 * 운영 프로파일은 {@code ddl-auto: validate} 라 여기서 어긋나면 부팅이 깨진다.
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
    @DisplayName("전 엔티티가 BaseEntity 를 상속한다")
    void everyEntityExtendsBaseEntity(Class<?> entityType) {
        assertThat(BaseEntity.class).isAssignableFrom(entityType);
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
