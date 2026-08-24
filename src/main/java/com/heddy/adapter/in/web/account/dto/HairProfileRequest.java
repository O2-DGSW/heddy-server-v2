package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.HairProfile.HairCondition;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.account.model.HairProfile.HairThickness;
import com.heddy.domain.account.model.HairProfile.HairType;
import com.heddy.domain.account.port.in.SaveHairProfileCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record HairProfileRequest(
        @NotNull @JsonProperty("hair_type") HairType hairType,
        @NotNull @JsonProperty("hair_condition") HairCondition hairCondition,
        @NotNull @JsonProperty("hair_length") HairLength hairLength,
        @NotNull @JsonProperty("hair_thickness") HairThickness hairThickness,
        @PositiveOrZero @JsonProperty("available_care_time_minutes")
        Integer availableCareTimeMinutes
) {
    public SaveHairProfileCommand toCommand(UUID userId) {
        return new SaveHairProfileCommand(userId, hairType, hairCondition, hairLength,
                hairThickness, availableCareTimeMinutes);
    }
}
