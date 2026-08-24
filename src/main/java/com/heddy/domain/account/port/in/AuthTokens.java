package com.heddy.domain.account.port.in;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static AuthTokens bearer(String accessToken, String refreshToken, long expiresIn) {
        return new AuthTokens(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
