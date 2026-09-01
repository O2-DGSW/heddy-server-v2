package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.ConsentDecision;
import com.heddy.domain.account.model.ConsentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "가입 시점의 약관 동의. 5개 필드를 모두 보내야 하며, 하나라도 빠지면 "
        + "400 이다. 필수 약관 2종에 동의하지 않으면 422 CONSENT_REQUIRED_NOT_GRANTED 로 "
        + "가입이 거부된다")
public record AgreementsRequest(
        @NotNull
        @Schema(description = "서비스 이용약관 동의. 필수 약관이라 true 여야 가입된다",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("terms_of_service") Boolean termsOfService,

        @NotNull
        @Schema(description = "개인정보 처리방침 동의. 필수 약관이라 true 여야 가입된다",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("privacy_policy") Boolean privacyPolicy,

        @NotNull
        @Schema(description = "AI 학습 활용 동의. 선택 약관이라 false 여도 가입된다. "
                + "서비스 분석 동의와 별개로 관리한다",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("ai_training") Boolean aiTraining,

        @NotNull
        @Schema(description = "서비스 분석 활용 동의. 선택 약관이라 false 여도 가입된다",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("service_analytics") Boolean serviceAnalytics,

        @NotNull
        @Schema(description = "마케팅 알림 수신 동의. 선택 약관이라 false 여도 가입된다",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("marketing_notification") Boolean marketingNotification
) {
    public List<ConsentDecision> toDecisions(String policyVersion) {
        return List.of(
                new ConsentDecision(ConsentType.TERMS_OF_SERVICE, termsOfService, policyVersion),
                new ConsentDecision(ConsentType.PRIVACY_POLICY, privacyPolicy, policyVersion),
                new ConsentDecision(ConsentType.AI_TRAINING, aiTraining, policyVersion),
                new ConsentDecision(ConsentType.SERVICE_ANALYTICS, serviceAnalytics, policyVersion),
                new ConsentDecision(ConsentType.MARKETING_NOTIFICATION, marketingNotification, policyVersion));
    }
}
