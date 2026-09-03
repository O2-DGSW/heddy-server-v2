package com.heddy.domain.style.port.in;

import com.heddy.domain.style.model.HairColor;
import com.heddy.domain.style.model.StyleTag;
import com.heddy.domain.style.model.StyleTagCategory;

import java.util.List;
import java.util.UUID;

public interface StyleUseCase {
    List<StyleTag> getStyleTags(StyleTagCategory category);

    /** 후보 스타일 저장과 AR 팔레트가 함께 쓰는 헤어 컬러 카탈로그를 읽는다. */
    List<HairColor> getHairColors();

    StylePreferencesResult getStylePreferences(UUID userId);

    StylePreferencesResult saveStylePreferences(SaveStylePreferencesCommand command);
}
