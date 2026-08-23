package com.heddy.adapter.in.web.account.controller;

import com.heddy.adapter.in.web.account.dto.ChangeConsentRequest;
import com.heddy.adapter.in.web.account.dto.ConsentStatusResponse;
import com.heddy.adapter.in.web.account.dto.ConsentsResponse;
import com.heddy.domain.account.model.ConsentType;
import com.heddy.domain.account.port.in.ConsentUseCase;
import com.heddy.global.filter.RequestIdFilter;
import com.heddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/me/consents")
@RequiredArgsConstructor
@Tag(name = "동의", description = "내 약관·데이터 활용·알림 동의 관리")
@SecurityRequirement(name = "bearerAuth")
public class ConsentController {

    private final ConsentUseCase consentUseCase;

    @GetMapping
    @Operation(summary = "내 동의 상태 조회", description = "동의 유형별 최신 변경 이력을 조회합니다.")
    public ApiResponse<ConsentsResponse> getConsents(
            @AuthenticationPrincipal UUID userId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                ConsentsResponse.from(consentUseCase.getConsents(userId)),
                RequestIdFilter.get(servletRequest));
    }

    @PutMapping("/{consentType}")
    @Operation(
            summary = "내 동의 상태 변경",
            description = "변경 이력을 추가합니다. 필수 약관 철회는 회원 탈퇴 흐름을 이용해야 합니다.")
    public ApiResponse<ConsentStatusResponse> changeConsent(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "변경할 동의 유형", required = true)
            @PathVariable ConsentType consentType,
            @Valid @RequestBody ChangeConsentRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                ConsentStatusResponse.from(
                        consentUseCase.changeConsent(request.toCommand(userId, consentType))),
                RequestIdFilter.get(servletRequest));
    }
}
