package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.port.in.ReauthenticateResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "재인증 응답. 탈퇴 같은 민감 작업에 이 토큰을 함께 보낸다")
public record ReauthenticateResponse(
        @Schema(description = "재인증 토큰. 한 번 쓰면 무효가 되며, 다시 쓰면 "
                + "401 AUTH_REAUTHENTICATION_TOKEN_REUSED 다")
        @JsonProperty("reauthentication_token") String reauthenticationToken,

        @Schema(description = "재인증 토큰의 남은 수명(초)", example = "300")
        @JsonProperty("expires_in") long expiresIn
) {
    public static ReauthenticateResponse from(ReauthenticateResult result) {
        return new ReauthenticateResponse(result.reauthenticationToken(), result.expiresIn());
    }
}
