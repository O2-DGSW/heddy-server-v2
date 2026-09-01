package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.AccountDeletionRequest;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "탈퇴 접수 응답. 탈퇴는 즉시 끝나지 않고 비동기로 처리되므로 접수만 "
        + "알리고 202 로 답한다")
public record AccountDeletionResponse(
        @Schema(description = "탈퇴 요청 식별자. 처리 상태를 문의할 때 쓴다")
        @JsonProperty("deletion_request_id") UUID deletionRequestId,

        @Schema(description = "탈퇴 처리 상태. 접수 직후에는 PROCESSING 이다",
                allowableValues = {"REQUESTED", "PROCESSING", "COMPLETED", "FAILED"})
        String status,

        @Schema(description = "탈퇴를 접수한 시각")
        @JsonProperty("requested_at") Instant requestedAt
) {
    public static AccountDeletionResponse from(AccountDeletionRequest request) {
        return new AccountDeletionResponse(
                request.deletionRequestId(), request.status().name(), request.requestedAt());
    }
}
