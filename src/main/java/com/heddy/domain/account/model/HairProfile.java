package com.heddy.domain.account.model;

import java.time.Instant;
import java.util.UUID;

public record HairProfile(
        UUID hairProfileId,
        UUID userId,
        HairType hairType,
        HairCondition hairCondition,
        HairLength hairLength,
        HairThickness hairThickness,
        Integer availableCareTimeMinutes,
        Instant createdAt,
        Instant updatedAt
) {
    public enum HairType { STRAIGHT, WAVY, CURLY }
    public enum HairCondition { HEALTHY, NORMAL, DAMAGED, SEVERELY_DAMAGED }
    public enum HairLength { SHORT, BELOW_CHIN, BELOW_SHOULDER, BELOW_CHEST }
    public enum HairThickness { THIN, NORMAL, THICK }

    public static HairProfile create(
            UUID userId,
            HairType hairType,
            HairCondition hairCondition,
            HairLength hairLength,
            HairThickness hairThickness,
            Integer availableCareTimeMinutes
    ) {
        return new HairProfile(UUID.randomUUID(), userId, hairType, hairCondition, hairLength,
                hairThickness, availableCareTimeMinutes, null, null);
    }

    public HairProfile replace(
            HairType hairType,
            HairCondition hairCondition,
            HairLength hairLength,
            HairThickness hairThickness,
            Integer availableCareTimeMinutes
    ) {
        return new HairProfile(hairProfileId, userId, hairType, hairCondition, hairLength,
                hairThickness, availableCareTimeMinutes, createdAt, updatedAt);
    }
}
