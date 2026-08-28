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

import static org.assertj.core.api.Assertions.assertThat;

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

    private void insertUser(UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, 'hash', 'EMAIL', 'ACTIVE', 0)
                """, userId, userId + "@example.com");
    }
}
