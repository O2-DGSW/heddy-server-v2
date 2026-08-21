package com.heddy.domain.style.port.in;

import com.heddy.domain.style.model.StyleTag;
import com.heddy.domain.style.model.StyleTagCategory;

import java.util.List;
import java.util.UUID;

public interface StyleUseCase {
    List<StyleTag> getStyleTags(StyleTagCategory category);

    StylePreferencesResult getStylePreferences(UUID userId);

    StylePreferencesResult saveStylePreferences(SaveStylePreferencesCommand command);
}
