package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.AccountDeletionRequest;

import java.time.Instant;
import java.util.UUID;

public record AccountDeletionResponse(
        @JsonProperty("deletion_request_id") UUID deletionRequestId,
        String status,
        @JsonProperty("requested_at") Instant requestedAt
) {
    public static AccountDeletionResponse from(AccountDeletionRequest request) {
        return new AccountDeletionResponse(
                request.deletionRequestId(), request.status().name(), request.requestedAt());
    }
}
