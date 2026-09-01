package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.ConsentStatus;
import com.heddy.domain.account.model.ConsentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "동의 유형 하나의 현재 상태")
public record ConsentStatusResponse(
        @Schema(description = "동의 유형. TERMS_OF_SERVICE 와 PRIVACY_POLICY 는 필수 약관이라 "
                + "철회할 수 없다")
        @JsonProperty("consent_type") ConsentType consentType,

        @Schema(description = "동의 여부")
        boolean granted,

        @Schema(description = "동의한 시점의 정책 버전. 현재 정책과 다르면 재동의가 필요하다")
        @JsonProperty("policy_version") String policyVersion,

        @Schema(description = "이 상태로 바뀐 시각. 변경 이력은 덮어쓰지 않고 행으로 쌓이며 "
                + "여기 보이는 건 가장 최근 한 건이다")
        @JsonProperty("changed_at") Instant changedAt
) {
    public static ConsentStatusResponse from(ConsentStatus status) {
        return new ConsentStatusResponse(
                status.type(), status.granted(), status.policyVersion(), status.changedAt());
    }
}
