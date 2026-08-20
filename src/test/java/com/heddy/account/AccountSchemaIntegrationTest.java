package com.heddy.account;

import com.heddy.account.entity.ConsentHistory;
import com.heddy.account.entity.ConsentSource;
import com.heddy.account.entity.ConsentType;
import com.heddy.account.entity.HairProfile;
import com.heddy.account.entity.HairType;
import com.heddy.account.entity.PreferenceType;
import com.heddy.account.entity.StyleTag;
import com.heddy.account.entity.StyleTagType;
import com.heddy.account.entity.User;
import com.heddy.account.entity.UserProfile;
import com.heddy.account.entity.UserStylePreference;
import com.heddy.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 PostgreSQL 16 에 Flyway 로 V1__init.sql 을 적용하고 {@code ddl-auto=validate} 로
 * 엔티티 매핑을 대조한다. 정적 텍스트 검사와 달리 DDL 실행·타입·길이·제약이 전부 진짜로 검증된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
@Testcontainers
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class AccountSchemaIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @PersistenceContext
    private EntityManager em;

    private JpaRepository<User, UUID> users;

    @BeforeEach
    void setUp() {
        users = new SimpleJpaRepository<>(User.class, em);
    }

    @Test
    @DisplayName("V1 적용 후 엔티티 매핑 검증(validate)이 통과한다")
    void migrationMatchesEntityMappings() {
        assertThat(em.getEntityManagerFactory()).isNotNull();
    }

    @Test
    @DisplayName("계정 그래프 전체가 저장된다")
    void persistsWholeAccountGraph() {
        User user = users.save(User.ofEmail("a@heddy.test", "hashed", "헤디"));
        em.persist(new UserProfile(user, "01012345678", "김디자이너", "두피가 예민함"));
        em.persist(HairProfile.unknownFor(user));
        StyleTag tag = new StyleTag("레이어드컷", StyleTagType.TREATMENT);
        em.persist(tag);
        em.persist(new UserStylePreference(user, tag, PreferenceType.PREFER));
        em.persist(new ConsentHistory(user, ConsentType.TERMS_OF_SERVICE, true, "1.0", ConsentSource.SIGNUP));

        em.flush();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(HairProfile.unknownFor(user).getHairType()).isEqualTo(HairType.UNKNOWN);
    }

    @Test
    @DisplayName("save() 가 merge 가 아닌 persist 경로를 타 원본 인스턴스가 관리 상태가 된다")
    void saveTakesPersistPath() {
        User user = User.ofEmail("persist@heddy.test", "hashed", "헤디");
        assertThat(user.isNew()).isTrue();

        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        User saved = users.save(user);
        em.flush();

        assertThat(saved).as("merge 경로였다면 복사본이 반환된다").isSameAs(user);
        assertThat(user.getCreatedAt()).as("원본이 관리 상태라 감사 필드가 채워진다").isNotNull();
        assertThat(user.isNew()).isFalse();
        assertThat(statistics.getEntityLoadCount()).as("신규 저장에 SELECT 가 선행하지 않는다").isZero();
        assertThat(statistics.getEntityInsertCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("조회한 엔티티는 신규로 판정되지 않는다")
    void loadedEntityIsNotNew() {
        User user = users.save(User.ofEmail("loaded@heddy.test", "hashed", "헤디"));
        em.flush();
        em.clear();

        User loaded = users.findById(user.getId()).orElseThrow();

        assertThat(loaded.isNew()).isFalse();
        assertThat(loaded.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("password_hash 없는 EMAIL 계정은 ck_users_credential 에 막힌다")
    void rejectsEmailAccountWithoutPassword() {
        assertThatThrownBy(() -> {
            em.createNativeQuery("""
                    INSERT INTO users (id, email, nickname, auth_provider, status, login_fail_count, created_at)
                    VALUES (?1, 'nopass@heddy.test', '헤디', 'EMAIL', 'ACTIVE', 0, now())
                    """).setParameter(1, UUID.randomUUID()).executeUpdate();
            em.flush();
        }).hasMessageContaining("ck_users_credential");
    }

    @Test
    @DisplayName("provider_subject 없는 소셜 계정은 ck_users_credential 에 막힌다")
    void rejectsSocialAccountWithoutProviderSubject() {
        assertThatThrownBy(() -> {
            em.createNativeQuery("""
                    INSERT INTO users (id, email, nickname, auth_provider, status, login_fail_count, created_at)
                    VALUES (?1, 'nosubject@heddy.test', '헤디', 'KAKAO', 'ACTIVE', 0, now())
                    """).setParameter(1, UUID.randomUUID()).executeUpdate();
            em.flush();
        }).hasMessageContaining("ck_users_credential");
    }

    @Test
    @DisplayName("범위 밖 tag_type 은 ck_style_tags_tag_type 에 막힌다")
    void rejectsUnknownStyleTagType() {
        assertThatThrownBy(() -> {
            em.createNativeQuery("""
                    INSERT INTO style_tags (id, tag_name, tag_type, created_at)
                    VALUES (?1, '알수없음', 'SOMETHING_ELSE', now())
                    """).setParameter(1, UUID.randomUUID()).executeUpdate();
            em.flush();
        }).hasMessageContaining("ck_style_tags_tag_type");
    }
}
