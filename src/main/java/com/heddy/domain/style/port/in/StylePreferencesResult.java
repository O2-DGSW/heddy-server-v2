package com.heddy.domain.style.port.in;

import com.heddy.domain.style.model.UserStylePreference;

import java.util.List;
import java.util.UUID;

public record StylePreferencesResult(
        List<UUID> preferredTagIds,
        List<UUID> excludedTagIds
) {
    public StylePreferencesResult {
        preferredTagIds = List.copyOf(preferredTagIds);
        excludedTagIds = List.copyOf(excludedTagIds);
    }

    public static StylePreferencesResult from(List<UserStylePreference> preferences) {
        List<UUID> preferred = preferences.stream()
                .filter(preference -> preference.preferenceType()
                        == UserStylePreference.PreferenceType.PREFERRED)
                .map(UserStylePreference::styleTagId)
                .sorted()
                .toList();
        List<UUID> excluded = preferences.stream()
                .filter(preference -> preference.preferenceType()
                        == UserStylePreference.PreferenceType.EXCLUDED)
                .map(UserStylePreference::styleTagId)
                .sorted()
                .toList();
        return new StylePreferencesResult(preferred, excluded);
    }
}
