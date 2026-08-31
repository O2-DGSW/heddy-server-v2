package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.HairProfile.HairCondition;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.account.model.HairProfile.HairThickness;
import com.heddy.domain.account.model.HairProfile.HairType;
import com.heddy.domain.account.port.in.SaveHairProfileCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

@Schema(description = "모발 프로필 저장 요청. 부분 수정이 아니라 전체 교체이므로 네 가지 "
        + "모발 특성을 매번 모두 보내야 한다")
public record HairProfileRequest(
        @NotNull
        @Schema(description = "모발 유형", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("hair_type") HairType hairType,

        @NotNull
        @Schema(description = "모발 상태", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("hair_condition") HairCondition hairCondition,

        @NotNull
        @Schema(description = "모발 길이", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("hair_length") HairLength hairLength,

        @NotNull
        @Schema(description = "모발 굵기", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("hair_thickness") HairThickness hairThickness,

        @PositiveOrZero
        @Schema(description = "하루에 머리 손질에 쓸 수 있는 시간(분). 선택 입력이며 0 이상. "
                + "스타일 추천이 관리 난이도를 맞출 때 쓴다", example = "15")
        @JsonProperty("available_care_time_minutes")
        Integer availableCareTimeMinutes
) {
    public SaveHairProfileCommand toCommand(UUID userId) {
        return new SaveHairProfileCommand(userId, hairType, hairCondition, hairLength,
                hairThickness, availableCareTimeMinutes);
    }
}
