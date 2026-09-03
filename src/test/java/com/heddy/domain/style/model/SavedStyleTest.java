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
    void createsACatalogBackedCandidateWithoutSnapshotFields() {
        UUID userId = UUID.randomUUID();
        UUID hairstyleId = UUID.randomUUID();
        UUID colorId = UUID.randomUUID();
        UUID captureId = UUID.randomUUID();

        SavedStyle savedStyle = SavedStyle.fromCatalog(
                userId, hairstyleId, "남자 다운펌", colorId, captureId, "  앞머리 살짝  ");

        assertThat(savedStyle.hairstyleId()).isEqualTo(hairstyleId);
        assertThat(savedStyle.colorId()).isEqualTo(colorId);
        assertThat(savedStyle.captureId()).isEqualTo(captureId);
        // 이름은 저장 시점 카탈로그 값의 스냅샷으로 남는다.
        assertThat(savedStyle.styleName()).isEqualTo("남자 다운펌");
        assertThat(savedStyle.memo()).isEqualTo("앞머리 살짝");
        // AR 저장에는 추천 이유가 없고, 이미지는 captureId 가 가리킨다.
        assertThat(savedStyle.imageUrl()).isNull();
        assertThat(savedStyle.reason()).isNull();
    }

    @Test
    void treatsBlankOptionalTextAsAbsent() {
        SavedStyle savedStyle = SavedStyle.fromCatalog(
                UUID.randomUUID(), UUID.randomUUID(), "남자 다운펌", null, null, "   ");

        assertThat(savedStyle.memo()).isNull();
        assertThat(savedStyle.colorId()).isNull();
        assertThat(savedStyle.captureId()).isNull();
    }

    @Test
    void rejectsAMemoLongerThanTheColumn() {
        assertThatThrownBy(() -> SavedStyle.fromCatalog(
                UUID.randomUUID(), UUID.randomUUID(), "남자 다운펌", null, null,
                "가".repeat(501)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresACatalogReference() {
        assertThatThrownBy(() -> SavedStyle.fromCatalog(
                UUID.randomUUID(), null, "남자 다운펌", null, null, null))
                .isInstanceOf(NullPointerException.class);
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
