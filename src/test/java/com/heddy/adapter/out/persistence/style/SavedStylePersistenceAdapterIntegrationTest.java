package com.heddy.adapter.out.persistence.style;

import com.heddy.domain.style.model.SavedStyle;
import com.heddy.domain.style.port.out.SavedStyleRepositoryPort;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class SavedStylePersistenceAdapterIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString(
            "85000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "85000000-0000-4000-8000-000000000002");

    @Autowired SavedStyleRepositoryPort repositoryPort;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpUsers() {
        insertUser(USER_ID);
        insertUser(OTHER_USER_ID);
    }

    @Test
    void insertsAndReadsOnlyStylesOwnedByTheRequestedUser() {
        SavedStyle saved = repositoryPort.insert(SavedStyle.create(
                USER_ID, "레이어드 커트", "https://images.example.com/layered.jpg",
                "이전 펌 이력 기반 추천"));

        List<SavedStyle> owned = repositoryPort.findAllByUserIdAndIds(
                USER_ID, List.of(saved.savedStyleId()));
        List<SavedStyle> foreign = repositoryPort.findAllByUserIdAndIds(
                OTHER_USER_ID, List.of(saved.savedStyleId()));

        assertThat(owned).singleElement().satisfies(style -> {
            assertThat(style.styleName()).isEqualTo("레이어드 커트");
            assertThat(style.imageUrl()).isEqualTo("https://images.example.com/layered.jpg");
            assertThat(style.reason()).isEqualTo("이전 펌 이력 기반 추천");
            assertThat(style.createdAt()).isNotNull();
        });
        assertThat(foreign).isEmpty();
    }

    @Test
    void createsExpectedColumnsIndexAndShareForeignKey() {
        Integer nameLength = jdbcTemplate.queryForObject("""
                SELECT character_maximum_length
                FROM information_schema.columns
                WHERE table_name = 'saved_styles' AND column_name = 'style_name'
                """, Integer.class);
        Integer imageLength = jdbcTemplate.queryForObject("""
                SELECT character_maximum_length
                FROM information_schema.columns
                WHERE table_name = 'saved_styles' AND column_name = 'image_url'
                """, Integer.class);
        Integer indexCount = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM pg_indexes
                WHERE tablename = 'saved_styles'
                  AND indexname = 'idx_saved_styles_user_created'
                """, Integer.class);
        Integer foreignKeyCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM information_schema.table_constraints
                WHERE table_name = 'share_saved_styles'
                  AND constraint_name = 'fk_share_saved_styles_saved_style'
                  AND constraint_type = 'FOREIGN KEY'
                """, Integer.class);

        assertThat(nameLength).isEqualTo(100);
        assertThat(imageLength).isEqualTo(2048);
        assertThat(indexCount).isOne();
        assertThat(foreignKeyCount).isOne();
    }

    @Test
    void roundTripsCatalogReferencesAndAllowsAnEmptySnapshot() {
        UUID hairstyleId = insertHairstyle("남자 다운펌");
        UUID colorId = UUID.fromString("c0100000-0000-4000-8000-000000000001");

        SavedStyle saved = repositoryPort.insert(SavedStyle.fromCatalog(
                USER_ID, hairstyleId, "남자 다운펌", colorId, null, "앞머리 살짝"));

        SavedStyle found = repositoryPort
                .findAllByUserIdAndIds(USER_ID, List.of(saved.savedStyleId()))
                .getFirst();
        assertThat(found.hairstyleId()).isEqualTo(hairstyleId);
        assertThat(found.colorId()).isEqualTo(colorId);
        assertThat(found.memo()).isEqualTo("앞머리 살짝");
        // AR 저장 경로에는 추천 스냅샷이 없다. 제약이 풀렸는지 실제 저장으로 확인한다.
        assertThat(found.imageUrl()).isNull();
        assertThat(found.reason()).isNull();

        assertThat(repositoryPort.findHairstyleIdsByUserId(USER_ID)).contains(hairstyleId);
    }

    @Test
    void refusesAColorThatIsNotInTheCatalog() {
        UUID hairstyleId = insertHairstyle("레이어드 컷");

        assertThatThrownBy(() -> repositoryPort.insert(SavedStyle.fromCatalog(
                USER_ID, hairstyleId, "레이어드 컷", UUID.randomUUID(), null, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID insertHairstyle(String styleName) {
        UUID hairstyleId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO hairstyle_assets (
                    hairstyle_id, style_name, category, asset_version
                ) VALUES (?, ?, 'CUT', 'v1')
                """, hairstyleId, styleName);
        return hairstyleId;
    }

    private void insertUser(UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, 'hash', 'EMAIL', 'ACTIVE', 0)
                """, userId, userId + "@example.com");
    }
}
