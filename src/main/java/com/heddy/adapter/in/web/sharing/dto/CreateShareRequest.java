package com.heddy.adapter.in.web.sharing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.sharing.model.ShareFieldType;
import com.heddy.domain.sharing.port.in.CreateShareUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;

/**
 * 공유 생성 요청. 대상·항목의 "1개 이상" 규칙은 교차 필드 조건이라 빈 값 검증을 여기 두지
 * 않는다 — null 을 빈 집합으로 정규화해 도메인 팩터리 한곳에서만 판정한다.
 */
public record CreateShareRequest(
        @Schema(description = "공유할 시술기록 식별자. 후보 스타일과 합쳐 1개 이상")
        @JsonProperty("record_ids") Set<UUID> recordIds,

        @Schema(description = "공유할 후보 스타일 식별자")
        @JsonProperty("saved_style_ids") Set<UUID> savedStyleIds,

        @Schema(description = "노출 항목 6종. 1개 이상", allowableValues = {"PHOTOS",
                "TREATMENT_DETAILS", "SATISFACTION", "CAUTIONS", "MEMO", "SAVED_STYLES"})
        Set<ShareFieldType> fields,

        @Schema(description = "유효기간(일). 비우면 7일")
        @JsonProperty("expires_in_days") Integer expiresInDays
) {

    public CreateShareRequest {
        recordIds = recordIds == null ? Set.of() : recordIds;
        savedStyleIds = savedStyleIds == null ? Set.of() : savedStyleIds;
        fields = fields == null ? Set.of() : fields;
    }

    public CreateShareUseCase.Command toCommand(UUID userId) {
        return new CreateShareUseCase.Command(
                userId, recordIds, savedStyleIds, fields, expiresInDays);
    }
}
