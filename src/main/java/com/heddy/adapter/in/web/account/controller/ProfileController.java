package com.heddy.adapter.in.web.account.controller;

import com.heddy.adapter.in.web.account.dto.HairProfileRequest;
import com.heddy.adapter.in.web.account.dto.AccountDeletionRequestDto;
import com.heddy.adapter.in.web.account.dto.AccountDeletionResponse;
import com.heddy.adapter.in.web.account.dto.HairProfileResponse;
import com.heddy.adapter.in.web.account.dto.MyProfileResponse;
import com.heddy.adapter.in.web.account.dto.UpdateMyProfileRequest;
import com.heddy.domain.account.port.in.ProfileUseCase;
import com.heddy.domain.account.port.in.RequestAccountDeletionUseCase;
import com.heddy.global.filter.RequestIdFilter;
import com.heddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@Tag(name = "프로필", description = "내 기본 프로필과 모발 프로필 관리")
public class ProfileController {

    private final ProfileUseCase profileUseCase;
    private final RequestAccountDeletionUseCase requestAccountDeletionUseCase;

    @GetMapping
    @Operation(summary = "내 기본 프로필 조회")
    public ApiResponse<MyProfileResponse> getProfile(
            @AuthenticationPrincipal UUID userId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(MyProfileResponse.from(profileUseCase.getProfile(userId)),
                RequestIdFilter.get(servletRequest));
    }

    @PatchMapping
    @Operation(summary = "내 기본 프로필 수정")
    public ApiResponse<MyProfileResponse> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateMyProfileRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                MyProfileResponse.from(profileUseCase.updateProfile(request.toCommand(userId))),
                RequestIdFilter.get(servletRequest));
    }

    @GetMapping("/hair-profile")
    @Operation(summary = "내 모발 프로필 조회")
    public ApiResponse<HairProfileResponse> getHairProfile(
            @AuthenticationPrincipal UUID userId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                HairProfileResponse.from(profileUseCase.getHairProfile(userId)),
                RequestIdFilter.get(servletRequest));
    }

    @PutMapping("/hair-profile")
    @Operation(summary = "내 모발 프로필 생성 또는 전체 수정")
    public ApiResponse<HairProfileResponse> saveHairProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody HairProfileRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                HairProfileResponse.from(profileUseCase.saveHairProfile(request.toCommand(userId))),
                RequestIdFilter.get(servletRequest));
    }

    @DeleteMapping
    @Operation(summary = "회원 탈퇴 요청",
            description = "재인증 토큰을 한 번 소비하고 모든 Refresh 세션과 계정 접근을 즉시 차단한 뒤 "
                    + "데이터 삭제를 비동기로 처리합니다.")
    public ResponseEntity<ApiResponse<AccountDeletionResponse>> deleteAccount(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody AccountDeletionRequestDto request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(
                AccountDeletionResponse.from(
                        requestAccountDeletionUseCase.request(request.toCommand(userId))),
                RequestIdFilter.get(servletRequest)));
    }
}
