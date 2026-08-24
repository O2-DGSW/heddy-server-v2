package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.HairProfile.HairCondition;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.account.model.HairProfile.HairThickness;
import com.heddy.domain.account.model.HairProfile.HairType;

import java.util.UUID;

public record SaveHairProfileCommand(
        UUID userId,
        HairType hairType,
        HairCondition hairCondition,
        HairLength hairLength,
        HairThickness hairThickness,
        Integer availableCareTimeMinutes
) {
}
