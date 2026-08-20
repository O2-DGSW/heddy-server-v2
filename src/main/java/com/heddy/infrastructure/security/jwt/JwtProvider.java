package com.heddy.infrastructure.security.jwt;

import com.heddy.domain.account.model.AccountRole;
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

    private static final String ROLE_CLAIM = "role";
    private static final String TYPE_CLAIM = "type";

    private final String secret;
    private final long accessTokenSeconds;
    private final long refreshTokenSeconds;
    private final Clock clock;
    private SecretKey signingKey;

    @Autowired
    public JwtProvider(
            @Value("${app.auth.jwt-secret}") String secret,
            @Value("${app.auth.access-token-seconds}") long accessTokenSeconds,
            @Value("${app.auth.refresh-token-seconds}") long refreshTokenSeconds
    ) {
        this(secret, accessTokenSeconds, refreshTokenSeconds, Clock.systemUTC());
    }

    JwtProvider(String secret, long accessTokenSeconds, long refreshTokenSeconds, Clock clock) {
        this.secret = secret;
        this.accessTokenSeconds = accessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
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
    public String createAccessToken(Long accountId, AccountRole role) {
        return createToken(accountId, role, JwtTokenType.ACCESS, accessTokenSeconds);
    }

    @Override
    public String createRefreshToken(Long accountId, AccountRole role) {
        return createToken(accountId, role, JwtTokenType.REFRESH, refreshTokenSeconds);
    }

    @Override
    public Optional<AuthPrincipal> parseAccessToken(String token) {
        return parse(token, JwtTokenType.ACCESS);
    }

    @Override
    public Optional<AuthPrincipal> parseRefreshToken(String token) {
        return parse(token, JwtTokenType.REFRESH);
    }

    private String createToken(
            Long accountId,
            AccountRole role,
            JwtTokenType type,
            long expiresInSeconds
    ) {
        Instant issuedAt = clock.instant();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(accountId.toString())
                .claim(ROLE_CLAIM, role.name())
                .claim(TYPE_CLAIM, type.value())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(expiresInSeconds)))
                .signWith(signingKey)
                .compact();
    }

    private Optional<AuthPrincipal> parse(String token, JwtTokenType expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.value().equals(claims.get(TYPE_CLAIM, String.class))) {
                return Optional.empty();
            }
            return Optional.of(new AuthPrincipal(
                    Long.valueOf(claims.getSubject()),
                    AccountRole.valueOf(claims.get(ROLE_CLAIM, String.class))));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
