package com.heddy.domain.style.model;

import java.util.UUID;

/**
 * 후보를 저장할 때 카탈로그에서 필요한 만큼만 떼어 온 스타일. 추천이 쓰는 후보 모델은
 * 추천 프로필까지 함께 들고 다니는데, 저장은 이름과 썸네일만 있으면 된다.
 */
public record CatalogHairstyle(
        UUID hairstyleId,
        String styleName,
        UUID thumbnailFileId
) {
}
