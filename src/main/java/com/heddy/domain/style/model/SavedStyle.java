package com.heddy.domain.style.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** AI 추천 결과 중 사용자가 보관한 후보 스타일의 스냅샷. */
public record SavedStyle(
        UUID savedStyleId,
        UUID userId,
        String styleName,
        String imageUrl,
        String reason,
        Instant createdAt
) {

    public SavedStyle {
        Objects.requireNonNull(savedStyleId, "savedStyleId");
        Objects.requireNonNull(userId, "userId");
        styleName = requireText(styleName, "styleName");
        imageUrl = requireText(imageUrl, "imageUrl");
        reason = requireText(reason, "reason");
    }

    public static SavedStyle create(
            UUID userId,
            String styleName,
            String imageUrl,
            String reason
    ) {
        return new SavedStyle(
                UUID.randomUUID(), userId, styleName, imageUrl, reason, null);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 비어 있을 수 없습니다.");
        }
        return value;
    }
}
