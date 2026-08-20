package com.heddy.infrastructure.security.social;

import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.port.out.SocialTokenVerifierPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@Component
public class SocialTokenVerifierAdapter implements SocialTokenVerifierPort {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final RestClient restClient;
    private final String googleClientId;
    private final String kakaoAppId;
    private final String appleClientId;
    private volatile JwtDecoder googleDecoder;
    private volatile JwtDecoder appleDecoder;

    public SocialTokenVerifierAdapter(
            @Value("${app.auth.social.google-client-id:}") String googleClientId,
            @Value("${app.auth.social.kakao-app-id:}") String kakaoAppId,
            @Value("${app.auth.social.apple-client-id:}") String appleClientId
    ) {
        this.restClient = RestClient.create();
        this.googleClientId = googleClientId;
        this.kakaoAppId = kakaoAppId;
        this.appleClientId = appleClientId;
    }

    @Override
    public Optional<VerifiedSocialIdentity> verify(AuthProvider provider, String providerToken) {
        if (providerToken == null || providerToken.isBlank()) {
            return Optional.empty();
        }
        try {
            return switch (provider) {
                case GOOGLE -> verifyJwt(googleDecoder(), providerToken, googleClientId);
                case APPLE -> verifyJwt(appleDecoder(), providerToken, appleClientId);
                case KAKAO -> verifyKakao(providerToken);
                case EMAIL -> Optional.empty();
            };
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<VerifiedSocialIdentity> verifyJwt(
            JwtDecoder decoder,
            String providerToken,
            String expectedAudience
    ) {
        if (expectedAudience.isBlank()) {
            return Optional.empty();
        }
        Jwt jwt = decoder.decode(providerToken);
        if (!jwt.getAudience().contains(expectedAudience) || jwt.getSubject() == null) {
            return Optional.empty();
        }
        return Optional.of(identity(jwt.getSubject()));
    }

    private Optional<VerifiedSocialIdentity> verifyKakao(String providerToken) {
        if (kakaoAppId.isBlank()) {
            return Optional.empty();
        }
        Map<?, ?> response = restClient.get()
                .uri("https://kapi.kakao.com/v1/user/access_token_info")
                .headers(headers -> headers.setBearerAuth(providerToken))
                .retrieve()
                .body(Map.class);
        if (response == null || response.get("id") == null || response.get("app_id") == null
                || !kakaoAppId.equals(String.valueOf(response.get("app_id")))) {
            return Optional.empty();
        }
        return Optional.of(identity(String.valueOf(response.get("id"))));
    }

    private VerifiedSocialIdentity identity(String subject) {
        return () -> subject;
    }

    private JwtDecoder googleDecoder() {
        if (googleDecoder == null) {
            synchronized (this) {
                if (googleDecoder == null) {
                    googleDecoder = JwtDecoders.fromIssuerLocation(GOOGLE_ISSUER);
                }
            }
        }
        return googleDecoder;
    }

    private JwtDecoder appleDecoder() {
        if (appleDecoder == null) {
            synchronized (this) {
                if (appleDecoder == null) {
                    appleDecoder = JwtDecoders.fromIssuerLocation(APPLE_ISSUER);
                }
            }
        }
        return appleDecoder;
    }
}
