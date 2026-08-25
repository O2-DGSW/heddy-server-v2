package com.heddy.infrastructure.security.jwt;

import com.heddy.domain.account.model.AuthPrincipal;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthTokenPort authTokenPort;
    private final ObjectProvider<AccountRepositoryPort> accountRepositoryPortProvider;

    public JwtAuthenticationFilter(
            AuthTokenPort authTokenPort,
            ObjectProvider<AccountRepositoryPort> accountRepositoryPortProvider
    ) {
        this.authTokenPort = authTokenPort;
        this.accountRepositoryPortProvider = accountRepositoryPortProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractBearerToken(request);
        if (token != null) {
            authTokenPort.parseAccessToken(token).ifPresent(this::authenticate);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(AuthPrincipal principal) {
        AccountRepositoryPort accountRepositoryPort = accountRepositoryPortProvider.getIfAvailable();
        if (accountRepositoryPort != null && accountRepositoryPort.findById(principal.userId())
                .map(account -> account.isDeleted())
                .orElse(true)) {
            return;
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal.userId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
