package com.heddy.infrastructure.security.oauth2;

import com.heddy.domain.account.port.in.AuthTokens;
import com.heddy.domain.account.port.in.SocialLoginUseCase;
import com.heddy.domain.account.port.out.PendingSocialLoginStorePort;
import com.heddy.domain.account.exception.AccountException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final SocialLoginUseCase socialLoginUseCase;
    private final PendingSocialLoginStorePort pendingSocialLoginStorePort;
    private final String redirectBaseUrl;
    private final long refreshTokenSeconds;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public OAuth2SuccessHandler(
            SocialLoginUseCase socialLoginUseCase,
            PendingSocialLoginStorePort pendingSocialLoginStorePort,
            @Value("${app.auth.oauth2.redirect-base-url}") String redirectBaseUrl,
            @Value("${app.auth.refresh-token-seconds}") long refreshTokenSeconds,
            @Value("${app.auth.cookie.secure:true}") boolean cookieSecure,
            @Value("${app.auth.cookie.same-site:None}") String cookieSameSite
    ) {
        this.socialLoginUseCase = socialLoginUseCase;
        this.pendingSocialLoginStorePort = pendingSocialLoginStorePort;
        this.redirectBaseUrl = redirectBaseUrl;
        this.refreshTokenSeconds = refreshTokenSeconds;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2AccountPrincipal principal = (OAuth2AccountPrincipal) authentication.getPrincipal();
        if (principal.isNewAccount()) {
            String pendingToken = UUID.randomUUID().toString();
            pendingSocialLoginStorePort.save(
                    pendingToken, principal.userInfo().provider(), principal.userInfo().providerId());
            response.sendRedirect(redirectBaseUrl + "/auth/social/register#pending=" + pendingToken);
            return;
        }

        AuthTokens tokens;
        try {
            tokens = socialLoginUseCase.login(principal.account());
        } catch (AccountException exception) {
            response.sendRedirect(redirectBaseUrl + "/auth/social/error");
            return;
        }
        ResponseCookie cookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/v1/auth/token/refresh")
                .maxAge(Duration.ofSeconds(refreshTokenSeconds))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        response.sendRedirect(redirectBaseUrl + "/auth/social/success#token=" + tokens.accessToken());
    }
}
