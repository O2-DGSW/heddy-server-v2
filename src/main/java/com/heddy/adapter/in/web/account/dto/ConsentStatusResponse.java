package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.ConsentSource;
import com.heddy.domain.account.model.ConsentStatus;
import com.heddy.domain.account.model.ConsentType;

import java.time.Instant;

public record ConsentStatusResponse(
        @JsonProperty("consent_type") ConsentType consentType,
        boolean granted,
        @JsonProperty("policy_version") String policyVersion,
        ConsentSource source,
        @JsonProperty("changed_at") Instant changedAt
) {
    public static ConsentStatusResponse from(ConsentStatus status) {
        return new ConsentStatusResponse(
                status.type(), status.granted(), status.policyVersion(),
                status.source(), status.changedAt());
    }
}
