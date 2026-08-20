package com.heddy.adapter.in.web.account.controller;

import com.heddy.config.TestSecurityConfig;
import com.heddy.domain.account.model.AccountRole;
import com.heddy.domain.account.port.in.AuthTokens;
import com.heddy.domain.account.port.in.LoginUseCase;
import com.heddy.domain.account.port.in.LogoutUseCase;
import com.heddy.domain.account.port.in.RefreshTokenUseCase;
import com.heddy.domain.account.port.in.ResetPasswordUseCase;
import com.heddy.domain.account.port.in.SendSmsCodeUseCase;
import com.heddy.domain.account.port.in.SignupAccountUseCase;
import com.heddy.domain.account.port.in.SocialSignupUseCase;
import com.heddy.domain.account.port.in.VerifySmsCodeUseCase;
import com.heddy.domain.account.port.out.AuthTokenPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        })
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AuthTokenPort authTokenPort;
    @MockitoBean SignupAccountUseCase signupAccountUseCase;
    @MockitoBean LoginUseCase loginUseCase;
    @MockitoBean LogoutUseCase logoutUseCase;
    @MockitoBean RefreshTokenUseCase refreshTokenUseCase;
    @MockitoBean ResetPasswordUseCase resetPasswordUseCase;
    @MockitoBean SendSmsCodeUseCase sendSmsCodeUseCase;
    @MockitoBean VerifySmsCodeUseCase verifySmsCodeUseCase;
    @MockitoBean SocialSignupUseCase socialSignupUseCase;

    @Test
    void signupReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"mola","password":"password!1","name":"몰라","phoneNumber":"01012345678"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void loginReturnsAccessTokenAndHttpOnlyRefreshCookie() throws Exception {
        given(loginUseCase.login(any())).willReturn(new AuthTokens("access", "refresh"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"mola","password":"password!1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("refresh_token=refresh"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("Path=/api/v1/auth/token/refresh"))));
    }

    @Test
    void refreshWithoutCookieReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_006"));
    }

    @Test
    void anonymousLogoutIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedLogoutDeletesSessionAndExpiresCookie() throws Exception {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        3L, null, List.of(new SimpleGrantedAuthority("ROLE_" + AccountRole.USER.name())));

        mockMvc.perform(post("/api/v1/auth/logout").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("Max-Age=0")));
        verify(logoutUseCase).logout(3L);
    }
}
