package com.heddy.infrastructure.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private final String redirectBaseUrl;

    public OAuth2FailureHandler(
            @Value("${app.auth.oauth2.redirect-base-url}") String redirectBaseUrl
    ) {
        this.redirectBaseUrl = redirectBaseUrl;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        log.warn("OAuth2 authentication failed");
        response.sendRedirect(redirectBaseUrl + "/auth/social/error");
    }
}
