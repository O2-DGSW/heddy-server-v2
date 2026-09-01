package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "토큰 재발급·로그아웃 요청")
public record RefreshTokenRequest(
        @NotBlank @Size(max = 512)
        @Schema(description = "로그인·가입 응답으로 받은 리프레시 토큰 원문. 최대 512자. "
                + "재발급에 성공하면 이 토큰은 즉시 무효가 되고 새 토큰이 내려온다. 이미 "
                + "회전된 토큰을 다시 쓰면 탈취로 보고 그 사용자의 세션을 모두 무효화한 뒤 "
                + "401 AUTH_REFRESH_TOKEN_REUSED 를 낸다 — 이때는 모든 기기에서 다시 "
                + "로그인해야 한다",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("refresh_token") String refreshToken
) {
}
