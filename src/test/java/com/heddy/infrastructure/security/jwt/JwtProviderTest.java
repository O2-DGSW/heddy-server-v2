package com.heddy.infrastructure.security.jwt;

import com.heddy.domain.account.model.AccountRole;
import com.heddy.domain.account.model.AuthPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-jwt-secret-must-have-at-least-32-bytes";
    private static final Instant NOW = Instant.parse("2026-08-20T00:00:00Z");

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
                SECRET, 3600, 2592000, Clock.fixed(NOW, ZoneOffset.UTC));
        jwtProvider.initialize();
    }

    @Test
    void accessTokenContainsAccountPrincipal() {
        String token = jwtProvider.createAccessToken(7L, AccountRole.USER);

        assertThat(jwtProvider.parseAccessToken(token))
                .contains(new AuthPrincipal(7L, AccountRole.USER));
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String token = jwtProvider.createRefreshToken(7L, AccountRole.USER);

        assertThat(jwtProvider.parseAccessToken(token)).isEmpty();
    }

    @Test
    void separatelyIssuedRefreshTokensAreUnique() {
        String first = jwtProvider.createRefreshToken(7L, AccountRole.USER);
        String second = jwtProvider.createRefreshToken(7L, AccountRole.USER);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void tokenWithDifferentSignatureIsRejected() {
        JwtProvider otherProvider = new JwtProvider(
                "another-test-secret-must-have-at-least-32-bytes", 3600, 2592000,
                Clock.fixed(NOW, ZoneOffset.UTC));
        otherProvider.initialize();
        String token = otherProvider.createAccessToken(7L, AccountRole.USER);

        assertThat(jwtProvider.parseAccessToken(token)).isEmpty();
    }
}
