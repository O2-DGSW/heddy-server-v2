package com.heddy.adapter.in.web.account.controller;

import com.heddy.config.TestSecurityConfig;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.port.in.AuthResult;
import com.heddy.domain.account.port.in.AuthTokens;
import com.heddy.domain.account.port.in.AuthUser;
import com.heddy.domain.account.port.in.CheckEmailAvailabilityUseCase;
import com.heddy.domain.account.port.in.EmailAvailabilityResult;
import com.heddy.domain.account.port.in.EmailLoginUseCase;
import com.heddy.domain.account.port.in.EmailSignupUseCase;
import com.heddy.domain.account.port.in.LogoutUseCase;
import com.heddy.domain.account.port.in.ReauthenticateUseCase;
import com.heddy.domain.account.port.in.RefreshTokenUseCase;
import com.heddy.domain.account.port.in.ResetPasswordUseCase;
import com.heddy.domain.account.port.in.SendSmsCodeUseCase;
import com.heddy.domain.account.port.in.SocialLoginUseCase;
import com.heddy.domain.account.port.in.SocialSignupUseCase;
import com.heddy.domain.account.port.in.VerifySmsCodeUseCase;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.global.error.GlobalExceptionHandler;
import com.heddy.global.filter.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(AuthController.class)
@Import({TestSecurityConfig.class, AccountExceptionHandler.class,
        GlobalExceptionHandler.class, RequestIdFilter.class})
class AuthControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @MockitoBean CheckEmailAvailabilityUseCase checkEmailAvailabilityUseCase;
    @MockitoBean EmailSignupUseCase emailSignupUseCase;
    @MockitoBean SocialSignupUseCase socialSignupUseCase;
    @MockitoBean EmailLoginUseCase emailLoginUseCase;
    @MockitoBean SocialLoginUseCase socialLoginUseCase;
    @MockitoBean RefreshTokenUseCase refreshTokenUseCase;
    @MockitoBean LogoutUseCase logoutUseCase;
    @MockitoBean ReauthenticateUseCase reauthenticateUseCase;
    @MockitoBean SendSmsCodeUseCase sendSmsCodeUseCase;
    @MockitoBean VerifySmsCodeUseCase verifySmsCodeUseCase;
    @MockitoBean ResetPasswordUseCase resetPasswordUseCase;
    @MockitoBean AuthTokenPort authTokenPort;

    @Test
    void emailAvailabilityUsesDocumentedEnvelope() throws Exception {
        given(checkEmailAvailabilityUseCase.check("user@example.com"))
                .willReturn(new EmailAvailabilityResult("user@example.com", true));

        mockMvc.perform(get("/auth/email-availability")
                        .param("email", "user@example.com")
                        .header(RequestIdFilter.HEADER, "request-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER, "request-1"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.request_id").value("request-1"));
    }

    @Test
    void emailSignupReturnsUuidUserAndSnakeCaseTokens() throws Exception {
        given(emailSignupUseCase.signup(any())).willReturn(authResult());

        mockMvc.perform(post("/auth/signup/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(RequestIdFilter.HEADER, "request-2")
                        .content("""
                                {
                                  "email":"user@example.com",
                                  "password":"Password123",
                                  "nickname":"헤디",
                                  "agreements":{
                                    "terms_of_service":true,
                                    "privacy_policy":true,
                                    "ai_training":false,
                                    "service_analytics":true,
                                    "marketing_notification":false
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.user_id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.tokens.access_token").value("access"))
                .andExpect(jsonPath("$.data.tokens.refresh_token").value("refresh"))
                .andExpect(jsonPath("$.data.tokens.token_type").value("Bearer"))
                .andExpect(jsonPath("$.data.tokens.expires_in").value(900))
                .andExpect(jsonPath("$.request_id").value("request-2"));
    }

    @Test
    void invalidSignupReturnsValidationEnvelope() throws Exception {
        mockMvc.perform(post("/auth/signup/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(RequestIdFilter.HEADER, "request-3")
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.field_errors").isArray())
                .andExpect(jsonPath("$.request_id").value("request-3"));
    }

    @Test
    void emailLoginMapsCurrentRequestContract() throws Exception {
        given(emailLoginUseCase.login(any())).willReturn(authResult());

        mockMvc.perform(post("/auth/login/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"user@example.com",
                                  "password":"Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.user_id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.tokens.access_token").value("access"));

        verify(emailLoginUseCase).login(org.mockito.ArgumentMatchers.argThat(command ->
                command.email().equals("user@example.com")
                        && command.password().equals("Password123")));
    }

    @Test
    void socialLoginMapsCurrentRequestContract() throws Exception {
        given(socialLoginUseCase.login(any())).willReturn(authResult());

        mockMvc.perform(post("/auth/login/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "provider_token":"provider-token"
                                }
                                """))
                .andExpect(status().isOk());

        verify(socialLoginUseCase).login(org.mockito.ArgumentMatchers.argThat(command ->
                command.provider() == AuthProvider.GOOGLE
                        && command.providerToken().equals("provider-token")));
    }

    @Test
    void logoutRevokesCurrentRefreshSessionAndReturnsNoContent() throws Exception {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(USER_ID, null, java.util.List.of());

        mockMvc.perform(post("/auth/logout")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"raw-refresh\"}"))
                .andExpect(status().isNoContent());

        verify(logoutUseCase).logout(USER_ID, "raw-refresh");
    }

    private AuthResult authResult() {
        return new AuthResult(
                new AuthUser(USER_ID, "user@example.com", "헤디", AccountStatus.ACTIVE),
                new AuthTokens("access", "refresh", "Bearer", 900));
    }
}
