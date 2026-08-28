package com.heddy.domain.style.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SavedStyleTest {

    @Test
    void createsAnOwnedSnapshotOfARecommendation() {
        UUID userId = UUID.randomUUID();

        SavedStyle savedStyle = SavedStyle.create(
                userId, "레이어드 커트", "https://images.example.com/layered.jpg",
                "이전 펌 이력 기반 추천");

        assertThat(savedStyle.savedStyleId()).isNotNull();
        assertThat(savedStyle.userId()).isEqualTo(userId);
        assertThat(savedStyle.styleName()).isEqualTo("레이어드 커트");
        assertThat(savedStyle.createdAt()).isNull();
    }

    @Test
    void rejectsBlankPublicContent() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> SavedStyle.create(
                userId, " ", "https://images.example.com/style.jpg", "추천 이유"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SavedStyle.create(
                userId, "스타일", "", "추천 이유"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SavedStyle.create(
                userId, "스타일", "https://images.example.com/style.jpg", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
