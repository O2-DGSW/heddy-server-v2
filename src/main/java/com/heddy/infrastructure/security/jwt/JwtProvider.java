package com.heddy.infrastructure.security.jwt;

import com.heddy.domain.account.model.AuthPrincipal;
import com.heddy.domain.account.port.out.AuthTokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtProvider implements AuthTokenPort {

    private static final String TYPE_CLAIM = "type";

    private final String secret;
    private final long accessTokenSeconds;
    private final long reauthenticationTokenSeconds;
    private final Clock clock;
    private SecretKey signingKey;

    @Autowired
    public JwtProvider(
            @Value("${app.auth.jwt-secret}") String secret,
            @Value("${app.auth.access-token-seconds}") long accessTokenSeconds,
            @Value("${app.auth.reauthentication-token-seconds}") long reauthenticationTokenSeconds
    ) {
        this(secret, accessTokenSeconds, reauthenticationTokenSeconds, Clock.systemUTC());
    }

    JwtProvider(String secret, long accessTokenSeconds, long reauthenticationTokenSeconds, Clock clock) {
        this.secret = secret;
        this.accessTokenSeconds = accessTokenSeconds;
        this.reauthenticationTokenSeconds = reauthenticationTokenSeconds;
        this.clock = clock;
    }

    @PostConstruct
    void initialize() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.auth.jwt-secret must be at least 32 bytes");
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String createAccessToken(UUID userId) {
        return createToken(userId, JwtTokenType.ACCESS, accessTokenSeconds);
    }

    @Override
    public String createReauthenticationToken(UUID userId) {
        return createToken(userId, JwtTokenType.REAUTHENTICATION, reauthenticationTokenSeconds);
    }

    @Override
    public Optional<AuthPrincipal> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!JwtTokenType.ACCESS.value().equals(claims.get(TYPE_CLAIM, String.class))) {
                return Optional.empty();
            }
            return Optional.of(new AuthPrincipal(UUID.fromString(claims.getSubject())));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String createToken(UUID userId, JwtTokenType type, long expiresInSeconds) {
        Instant issuedAt = clock.instant();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim(TYPE_CLAIM, type.value())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(expiresInSeconds)))
                .signWith(signingKey)
                .compact();
    }
}
