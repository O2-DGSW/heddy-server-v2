package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.port.in.ReauthenticateResult;

public record ReauthenticateResponse(
        @JsonProperty("reauthentication_token") String reauthenticationToken,
        @JsonProperty("expires_in") long expiresIn
) {
    public static ReauthenticateResponse from(ReauthenticateResult result) {
        return new ReauthenticateResponse(result.reauthenticationToken(), result.expiresIn());
    }
}
