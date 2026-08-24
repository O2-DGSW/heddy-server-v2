package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.port.in.MyProfileResult;

import java.time.Instant;
import java.util.UUID;

public record MyProfileResponse(
        @JsonProperty("user_id") UUID userId,
        String email,
        String nickname,
        String phone,
        @JsonProperty("preferred_designer") String preferredDesigner,
        @JsonProperty("hair_cautions") String hairCautions,
        AccountStatus status,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static MyProfileResponse from(MyProfileResult result) {
        return new MyProfileResponse(result.userId(), result.email(), result.nickname(),
                result.phone(), result.preferredDesigner(), result.hairCautions(), result.status(),
                result.createdAt(), result.updatedAt());
    }
}
