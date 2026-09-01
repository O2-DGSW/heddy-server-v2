package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.account.model.HairProfile.HairCondition;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.account.model.HairProfile.HairThickness;
import com.heddy.domain.account.model.HairProfile.HairType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "모발 프로필 응답. 저장한 적이 없으면 조회는 "
        + "404 HAIR_PROFILE_NOT_FOUND 다")
public record HairProfileResponse(
        @Schema(description = "모발 유형")
        @JsonProperty("hair_type") HairType hairType,

        @Schema(description = "모발 상태")
        @JsonProperty("hair_condition") HairCondition hairCondition,

        @Schema(description = "모발 길이")
        @JsonProperty("hair_length") HairLength hairLength,

        @Schema(description = "모발 굵기")
        @JsonProperty("hair_thickness") HairThickness hairThickness,

        @Schema(description = "하루에 머리 손질에 쓸 수 있는 시간(분). 입력하지 않았으면 비어 있다",
                example = "15")
        @JsonProperty("available_care_time_minutes") Integer availableCareTimeMinutes,

        @Schema(description = "프로필을 처음 저장한 시각")
        @JsonProperty("created_at") Instant createdAt,

        @Schema(description = "마지막으로 저장한 시각")
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static HairProfileResponse from(HairProfile profile) {
        return new HairProfileResponse(profile.hairType(), profile.hairCondition(),
                profile.hairLength(), profile.hairThickness(),
                profile.availableCareTimeMinutes(), profile.createdAt(), profile.updatedAt());
    }
}
