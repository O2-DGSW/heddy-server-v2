package com.heddy.adapter.in.web.account.controller;

import com.heddy.adapter.in.web.account.dto.LoginRequest;
import com.heddy.adapter.in.web.account.dto.LoginResponse;
import com.heddy.adapter.in.web.account.dto.ResetPasswordRequest;
import com.heddy.adapter.in.web.account.dto.SendSmsCodeRequest;
import com.heddy.adapter.in.web.account.dto.SignupRequest;
import com.heddy.adapter.in.web.account.dto.SocialSignupRequest;
import com.heddy.adapter.in.web.account.dto.VerifySmsCodeRequest;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.port.in.AuthTokens;
import com.heddy.domain.account.port.in.LoginUseCase;
import com.heddy.domain.account.port.in.LogoutUseCase;
import com.heddy.domain.account.port.in.RefreshTokenUseCase;
import com.heddy.domain.account.port.in.ResetPasswordUseCase;
import com.heddy.domain.account.port.in.SendSmsCodeUseCase;
import com.heddy.domain.account.port.in.SignupAccountUseCase;
import com.heddy.domain.account.port.in.SocialSignupUseCase;
import com.heddy.domain.account.port.in.VerifySmsCodeUseCase;
import com.heddy.global.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SignupAccountUseCase signupAccountUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final SendSmsCodeUseCase sendSmsCodeUseCase;
    private final VerifySmsCodeUseCase verifySmsCodeUseCase;
    private final SocialSignupUseCase socialSignupUseCase;
    private final long refreshTokenSeconds;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(
            SignupAccountUseCase signupAccountUseCase,
            LoginUseCase loginUseCase,
            LogoutUseCase logoutUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            SendSmsCodeUseCase sendSmsCodeUseCase,
            VerifySmsCodeUseCase verifySmsCodeUseCase,
            SocialSignupUseCase socialSignupUseCase,
            @Value("${app.auth.refresh-token-seconds}") long refreshTokenSeconds,
            @Value("${app.auth.cookie.secure:true}") boolean cookieSecure,
            @Value("${app.auth.cookie.same-site:None}") String cookieSameSite
    ) {
        this.signupAccountUseCase = signupAccountUseCase;
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.sendSmsCodeUseCase = sendSmsCodeUseCase;
        this.verifySmsCodeUseCase = verifySmsCodeUseCase;
        this.socialSignupUseCase = socialSignupUseCase;
        this.refreshTokenSeconds = refreshTokenSeconds;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        signupAccountUseCase.signup(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return tokenResponse(loginUseCase.login(request.toCommand()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Long accountId) {
        logoutUseCase.logout(accountId);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .body(ApiResponse.message("로그아웃 되었습니다."));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest request) {
        String refreshToken = extractRefreshToken(request);
        if (refreshToken == null) {
            throw new AccountException(AccountError.INVALID_REFRESH_TOKEN);
        }
        return tokenResponse(refreshTokenUseCase.refresh(refreshToken));
    }

    @PostMapping("/social/signup")
    public ResponseEntity<ApiResponse<LoginResponse>> socialSignup(
            @Valid @RequestBody SocialSignupRequest request
    ) {
        return tokenResponse(socialSignupUseCase.signup(request.toCommand()));
    }

    @PostMapping("/sms/send")
    public ApiResponse<Void> sendSmsCode(@Valid @RequestBody SendSmsCodeRequest request) {
        sendSmsCodeUseCase.send(request.toCommand());
        return ApiResponse.message("인증 코드가 발송되었습니다.");
    }

    @PostMapping("/sms/verify")
    public ApiResponse<Void> verifySmsCode(@Valid @RequestBody VerifySmsCodeRequest request) {
        verifySmsCodeUseCase.verify(request.toCommand());
        return ApiResponse.message("전화번호 인증이 완료되었습니다.");
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.reset(request.toCommand());
        return ApiResponse.message("비밀번호가 변경되었습니다.");
    }

    private ResponseEntity<ApiResponse<LoginResponse>> tokenResponse(AuthTokens tokens) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString())
                .body(ApiResponse.success(new LoginResponse(tokens.accessToken())));
    }

    private ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/v1/auth/token/refresh")
                .maxAge(Duration.ofSeconds(refreshTokenSeconds))
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/v1/auth/token/refresh")
                .maxAge(Duration.ZERO)
                .build();
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> "refresh_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
