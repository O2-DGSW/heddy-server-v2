package com.heddy.domain.style.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 사용자가 보관한 후보 스타일. 카탈로그가 아니라 사용자별 보관함이며, 공용 카탈로그
 * ({@code hairstyle_assets} · {@code hair_colors})를 가리키는 참조와 저장 당시의 스냅샷을
 * 함께 들고 있다.
 *
 * <p>이름을 스냅샷으로 남기는 이유는 공유가 이 행을 가리키기 때문이다. 카탈로그에서 스타일이
 * 내려가거나 이름이 바뀌어도 이미 나간 공유 링크는 저장 당시의 이름을 그대로 보여야 한다.
 *
 * <p>{@code imageUrl} 과 {@code reason} 은 AI 추천 스냅샷 시절의 자리라 비어 있을 수 있다.
 * AR 에서 저장한 후보에는 추천 이유가 없고, 이미지도 URL 문자열이 아니라 {@code captureId} 가
 * 가리키는 파일이다. 조회 시점에 짧은 만료의 서명 URL 을 새로 발급하므로 URL 을 보관하지 않는다.
 */
public record SavedStyle(
        UUID savedStyleId,
        UUID userId,
        String styleName,
        String imageUrl,
        String reason,
        UUID hairstyleId,
        UUID colorId,
        UUID captureId,
        String memo,
        Instant createdAt
) {
    private static final int MEMO_MAX_LENGTH = 500;

    public SavedStyle {
        Objects.requireNonNull(savedStyleId, "savedStyleId");
        Objects.requireNonNull(userId, "userId");
        styleName = requireText(styleName, "styleName");
        imageUrl = normalizeText(imageUrl);
        reason = normalizeText(reason);
        memo = normalizeText(memo);
        if (memo != null && memo.length() > MEMO_MAX_LENGTH) {
            throw new IllegalArgumentException("memo는 " + MEMO_MAX_LENGTH + "자를 넘을 수 없습니다.");
        }
    }

    /** 카탈로그를 가리키는 후보를 만든다. 이름은 저장 시점의 카탈로그 값을 그대로 옮겨 둔다. */
    public static SavedStyle fromCatalog(
            UUID userId,
            UUID hairstyleId,
            String styleName,
            UUID colorId,
            UUID captureId,
            String memo
    ) {
        Objects.requireNonNull(hairstyleId, "hairstyleId");
        return new SavedStyle(
                UUID.randomUUID(), userId, styleName, null, null,
                hairstyleId, colorId, captureId, memo, null);
    }

    /** AI 추천 스냅샷으로 만드는 기존 경로. 카탈로그 참조가 없다. */
    public static SavedStyle create(
            UUID userId,
            String styleName,
            String imageUrl,
            String reason
    ) {
        return new SavedStyle(
                UUID.randomUUID(), userId, styleName,
                requireText(imageUrl, "imageUrl"), requireText(reason, "reason"),
                null, null, null, null, null);
    }

    public SavedStyle updateMemo(String updatedMemo) {
        return new SavedStyle(
                savedStyleId, userId, styleName, imageUrl, reason,
                hairstyleId, colorId, captureId, updatedMemo, createdAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 비어 있을 수 없습니다.");
        }
        return value.strip();
    }

    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
