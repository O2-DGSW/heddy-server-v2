package com.heddy.adapter.in.web.account.controller;

import com.heddy.adapter.in.web.account.dto.AuthResponse;
import com.heddy.adapter.in.web.account.dto.EmailAvailabilityResponse;
import com.heddy.adapter.in.web.account.dto.EmailLoginRequest;
import com.heddy.adapter.in.web.account.dto.EmailSignupRequest;
import com.heddy.adapter.in.web.account.dto.ReauthenticateRequest;
import com.heddy.adapter.in.web.account.dto.ReauthenticateResponse;
import com.heddy.adapter.in.web.account.dto.RefreshTokenRequest;
import com.heddy.adapter.in.web.account.dto.ResetPasswordRequest;
import com.heddy.adapter.in.web.account.dto.SendSmsCodeRequest;
import com.heddy.adapter.in.web.account.dto.SocialLoginRequest;
import com.heddy.adapter.in.web.account.dto.SocialSignupRequest;
import com.heddy.adapter.in.web.account.dto.VerifySmsCodeRequest;
import com.heddy.domain.account.port.in.CheckEmailAvailabilityUseCase;
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
import com.heddy.global.filter.RequestIdFilter;
import com.heddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/auth")
@Tag(name = "인증", description = "회원가입, 로그인, 토큰, 재인증 및 휴대전화 인증")
public class AuthController {

    private final CheckEmailAvailabilityUseCase checkEmailAvailabilityUseCase;
    private final EmailSignupUseCase emailSignupUseCase;
    private final SocialSignupUseCase socialSignupUseCase;
    private final EmailLoginUseCase emailLoginUseCase;
    private final SocialLoginUseCase socialLoginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ReauthenticateUseCase reauthenticateUseCase;
    private final SendSmsCodeUseCase sendSmsCodeUseCase;
    private final VerifySmsCodeUseCase verifySmsCodeUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final String consentPolicyVersion;

    public AuthController(
            CheckEmailAvailabilityUseCase checkEmailAvailabilityUseCase,
            EmailSignupUseCase emailSignupUseCase,
            SocialSignupUseCase socialSignupUseCase,
            EmailLoginUseCase emailLoginUseCase,
            SocialLoginUseCase socialLoginUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase,
            ReauthenticateUseCase reauthenticateUseCase,
            SendSmsCodeUseCase sendSmsCodeUseCase,
            VerifySmsCodeUseCase verifySmsCodeUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            @Value("${app.auth.consent-policy-version}") String consentPolicyVersion
    ) {
        this.checkEmailAvailabilityUseCase = checkEmailAvailabilityUseCase;
        this.emailSignupUseCase = emailSignupUseCase;
        this.socialSignupUseCase = socialSignupUseCase;
        this.emailLoginUseCase = emailLoginUseCase;
        this.socialLoginUseCase = socialLoginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.reauthenticateUseCase = reauthenticateUseCase;
        this.sendSmsCodeUseCase = sendSmsCodeUseCase;
        this.verifySmsCodeUseCase = verifySmsCodeUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.consentPolicyVersion = consentPolicyVersion;
    }

    @GetMapping("/email-availability")
    @Operation(summary = "이메일 중복 확인")
    public ApiResponse<EmailAvailabilityResponse> emailAvailability(
            @RequestParam @NotBlank @Email @Size(max = 255) String email,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                EmailAvailabilityResponse.from(
                        checkEmailAvailabilityUseCase.check(email.toLowerCase())),
                RequestIdFilter.get(servletRequest));
    }

    @PostMapping("/signup/email")
    @Operation(summary = "이메일 회원가입",
            description = "전화번호를 전달하는 경우 SIGNUP 목적의 SMS 인증이 먼저 완료되어야 합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "회원가입 성공")
    public ResponseEntity<ApiResponse<AuthResponse>> emailSignup(
            @Valid @RequestBody EmailSignupRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                AuthResponse.from(emailSignupUseCase.signup(request.toCommand(consentPolicyVersion))),
                RequestIdFilter.get(servletRequest)));
    }

    @PostMapping("/signup/social")
    @Operation(summary = "소셜 회원가입",
            description = "전화번호를 전달하는 경우 SIGNUP 목적의 SMS 인증이 먼저 완료되어야 합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "회원가입 성공")
    public ResponseEntity<ApiResponse<AuthResponse>> socialSignup(
            @Valid @RequestBody SocialSignupRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                AuthResponse.from(socialSignupUseCase.signup(request.toCommand(consentPolicyVersion))),
                RequestIdFilter.get(servletRequest)));
    }

    @PostMapping("/login/email")
    @Operation(summary = "이메일 로그인")
    public ApiResponse<AuthResponse> emailLogin(
            @Valid @RequestBody EmailLoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                AuthResponse.from(emailLoginUseCase.login(request.toCommand())),
                RequestIdFilter.get(servletRequest));
    }

    @PostMapping("/login/social")
    @Operation(summary = "소셜 로그인")
    public ApiResponse<AuthResponse> socialLogin(
            @Valid @RequestBody SocialLoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                AuthResponse.from(socialLoginUseCase.login(request.toCommand())),
                RequestIdFilter.get(servletRequest));
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "Access Token 갱신",
            description = "Refresh Token을 회전하고 새 Access Token과 Refresh Token을 발급합니다.")
    public ApiResponse<AuthResponse.Tokens> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                AuthResponse.Tokens.from(refreshTokenUseCase.refresh(request.refreshToken())),
                RequestIdFilter.get(servletRequest));
    }

    @PostMapping("/logout")
    @Operation(summary = "현재 세션 로그아웃")
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204", description = "로그아웃 성공")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        logoutUseCase.logout(userId, request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reauthenticate")
    @Operation(summary = "민감 작업 재인증",
            description = "회원 탈퇴 등에 사용할 수 있는 300초 유효 1회용 토큰을 발급합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ReauthenticateResponse> reauthenticate(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ReauthenticateRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                ReauthenticateResponse.from(reauthenticateUseCase.reauthenticate(request.toCommand(userId))),
                RequestIdFilter.get(servletRequest));
    }

    @PostMapping("/sms/send")
    @Operation(summary = "SMS 인증번호 발송",
            description = "SIGNUP, PASSWORD_RESET, PHONE_CHANGE 목적별 인증번호를 발송합니다.")
    public ApiResponse<Void> sendSmsCode(
            @Valid @RequestBody SendSmsCodeRequest request,
            HttpServletRequest servletRequest
    ) {
        sendSmsCodeUseCase.send(request.toCommand());
        return ApiResponse.success(null, RequestIdFilter.get(servletRequest));
    }

    @PostMapping("/sms/verify")
    @Operation(summary = "SMS 인증번호 확인")
    public ApiResponse<Void> verifySmsCode(
            @Valid @RequestBody VerifySmsCodeRequest request,
            HttpServletRequest servletRequest
    ) {
        verifySmsCodeUseCase.verify(request.toCommand());
        return ApiResponse.success(null, RequestIdFilter.get(servletRequest));
    }

    @PostMapping("/password/reset")
    @Operation(summary = "비밀번호 재설정",
            description = "PASSWORD_RESET 목적의 SMS 인증이 완료된 전화번호의 비밀번호를 변경합니다.")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        resetPasswordUseCase.reset(request.toCommand());
        return ApiResponse.success(null, RequestIdFilter.get(servletRequest));
    }
}
