package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.port.in.AuthResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "가입·로그인 성공 응답. 계정 정보와 토큰 한 쌍을 함께 돌려준다")
public record AuthResponse(
        @Schema(description = "로그인한 계정") User user,
        @Schema(description = "발급된 토큰") Tokens tokens
) {

    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(
                new User(result.user().userId(), result.user().email(),
                        result.user().nickname(), result.user().status()),
                Tokens.from(result.tokens()));
    }

    @Schema(name = "AuthUser")
    public record User(
            @Schema(description = "계정 식별자")
            @JsonProperty("user_id") UUID userId,

            @Schema(description = "계정 이메일. 소문자로 정규화된 값이다")
            String email,

            @Schema(description = "표시 이름")
            String nickname,

            @Schema(description = "계정 상태. DELETION_PENDING 은 탈퇴 요청 후 처리 대기 중이고, "
                    + "LOCKED 는 로그인 실패 누적으로 잠긴 상태다",
                    allowableValues = {"ACTIVE", "LOCKED", "DELETION_PENDING", "DELETED"})
            AccountStatus status
    ) {
    }

    @Schema(name = "AuthTokens")
    public record Tokens(
            @Schema(description = "액세스 토큰. Authorization 헤더에 "
                    + "\"Bearer {access_token}\" 형태로 싣는다")
            @JsonProperty("access_token") String accessToken,

            @Schema(description = "리프레시 토큰 원문. 서버는 해시만 저장하므로 이 값을 잃으면 "
                    + "재발급할 수 없다. 재발급 때마다 새 값으로 바뀌며 이전 값은 즉시 무효다")
            @JsonProperty("refresh_token") String refreshToken,

            @Schema(description = "토큰 유형. 항상 Bearer 다", example = "Bearer")
            @JsonProperty("token_type") String tokenType,

            @Schema(description = "액세스 토큰의 남은 수명(초). 리프레시 토큰의 수명이 아니다",
                    example = "3600")
            @JsonProperty("expires_in") long expiresIn
    ) {
        public static Tokens from(com.heddy.domain.account.port.in.AuthTokens tokens) {
            return new Tokens(tokens.accessToken(), tokens.refreshToken(),
                    tokens.tokenType(), tokens.expiresIn());
        }
    }
}
