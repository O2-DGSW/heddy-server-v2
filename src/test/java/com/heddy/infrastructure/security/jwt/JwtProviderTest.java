package com.heddy.infrastructure.security.jwt;

import com.heddy.domain.account.model.AuthPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-jwt-secret-must-have-at-least-32-bytes";
    private static final UUID USER_ID = UUID.randomUUID();
    private JwtProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtProvider(SECRET, 900, 300,
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC));
        provider.initialize();
    }

    @Test
    void parsesValidAccessTokenWithUuidSubject() {
        String token = provider.createAccessToken(USER_ID);
        assertThat(provider.parseAccessToken(token)).contains(new AuthPrincipal(USER_ID));
    }

    @Test
    void reauthenticationTokenCannotAuthenticateApiRequest() {
        String token = provider.createReauthenticationToken(USER_ID);
        assertThat(provider.parseAccessToken(token)).isEmpty();
        assertThat(provider.parseReauthenticationToken(token))
                .hasValueSatisfying(principal -> {
                    assertThat(principal.userId()).isEqualTo(USER_ID);
                    assertThat(principal.tokenId()).isNotNull();
                });
    }

    @Test
    void tokenSignedByDifferentKeyIsRejected() {
        JwtProvider other = new JwtProvider(
                "another-test-jwt-secret-with-at-least-32-bytes", 900, 300,
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC));
        other.initialize();
        assertThat(provider.parseAccessToken(other.createAccessToken(USER_ID))).isEmpty();
    }
}
