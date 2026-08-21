package com.heddy.domain.style.model;

import java.time.Instant;
import java.util.UUID;

public record UserStylePreference(
        UUID preferenceId,
        UUID userId,
        UUID styleTagId,
        PreferenceType preferenceType,
        Instant createdAt
) {
    public enum PreferenceType {
        PREFERRED,
        EXCLUDED
    }

    public static UserStylePreference create(
            UUID userId,
            UUID styleTagId,
            PreferenceType preferenceType
    ) {
        return new UserStylePreference(
                UUID.randomUUID(), userId, styleTagId, preferenceType, null);
    }
}
