package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.port.in.AuthResult;

import java.util.UUID;

public record AuthResponse(User user, Tokens tokens) {

    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(
                new User(result.user().userId(), result.user().email(),
                        result.user().nickname(), result.user().status()),
                Tokens.from(result.tokens()));
    }

    public record User(
            @JsonProperty("user_id") UUID userId,
            String email,
            String nickname,
            AccountStatus status
    ) {
    }

    public record Tokens(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn
    ) {
        public static Tokens from(com.heddy.domain.account.port.in.AuthTokens tokens) {
            return new Tokens(tokens.accessToken(), tokens.refreshToken(),
                    tokens.tokenType(), tokens.expiresIn());
        }
    }
}
