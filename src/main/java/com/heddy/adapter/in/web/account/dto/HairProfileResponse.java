package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.account.model.HairProfile.HairCondition;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.account.model.HairProfile.HairThickness;
import com.heddy.domain.account.model.HairProfile.HairType;

import java.time.Instant;

public record HairProfileResponse(
        @JsonProperty("hair_type") HairType hairType,
        @JsonProperty("hair_condition") HairCondition hairCondition,
        @JsonProperty("hair_length") HairLength hairLength,
        @JsonProperty("hair_thickness") HairThickness hairThickness,
        @JsonProperty("available_care_time_minutes") Integer availableCareTimeMinutes,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static HairProfileResponse from(HairProfile profile) {
        return new HairProfileResponse(profile.hairType(), profile.hairCondition(),
                profile.hairLength(), profile.hairThickness(),
                profile.availableCareTimeMinutes(), profile.createdAt(), profile.updatedAt());
    }
}
