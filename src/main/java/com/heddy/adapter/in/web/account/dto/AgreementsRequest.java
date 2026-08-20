package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.ConsentDecision;
import com.heddy.domain.account.model.ConsentType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AgreementsRequest(
        @NotNull @JsonProperty("terms_of_service") Boolean termsOfService,
        @NotNull @JsonProperty("privacy_policy") Boolean privacyPolicy,
        @NotNull @JsonProperty("ai_training") Boolean aiTraining,
        @NotNull @JsonProperty("service_analytics") Boolean serviceAnalytics,
        @NotNull @JsonProperty("marketing_notification") Boolean marketingNotification
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
