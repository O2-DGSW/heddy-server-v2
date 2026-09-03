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
        String memo,
        Instant createdAt,
        Instant updatedAt
) {

    private static final int STYLE_NAME_MAX_LENGTH = 100;
    private static final int IMAGE_URL_MAX_LENGTH = 2048;
    private static final int REASON_MAX_LENGTH = 500;
    private static final int MEMO_MAX_LENGTH = 500;

    public SavedStyle {
        Objects.requireNonNull(savedStyleId, "savedStyleId");
        Objects.requireNonNull(userId, "userId");
        styleName = requireText(styleName, "styleName", STYLE_NAME_MAX_LENGTH);
        imageUrl = requireText(imageUrl, "imageUrl", IMAGE_URL_MAX_LENGTH);
        reason = requireText(reason, "reason", REASON_MAX_LENGTH);
        memo = normalizeOptionalText(memo, "memo", MEMO_MAX_LENGTH);
    }

    /** 메모 필드가 생기기 전 공유 스냅샷 호출부와의 호환 생성자. */
    public SavedStyle(
            UUID savedStyleId,
            UUID userId,
            String styleName,
            String imageUrl,
            String reason,
            Instant createdAt
    ) {
        this(savedStyleId, userId, styleName, imageUrl, reason, null, createdAt, createdAt);
    }

    public static SavedStyle create(
            UUID userId,
            String styleName,
            String imageUrl,
            String reason,
            String memo
    ) {
        return new SavedStyle(
                UUID.randomUUID(), userId, styleName, imageUrl, reason, memo, null, null);
    }

    public static SavedStyle create(
            UUID userId,
            String styleName,
            String imageUrl,
            String reason
    ) {
        return create(userId, styleName, imageUrl, reason, null);
    }

    public SavedStyle updateMemo(String memo) {
        return new SavedStyle(savedStyleId, userId, styleName, imageUrl, reason,
                memo, createdAt, updatedAt);
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 비어 있을 수 없습니다.");
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "의 길이가 제한을 초과했습니다.");
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "의 길이가 제한을 초과했습니다.");
        }
        return normalized;
    }
}
