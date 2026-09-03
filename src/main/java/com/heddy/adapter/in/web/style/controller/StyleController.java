package com.heddy.adapter.in.web.style.controller;

import com.heddy.adapter.in.web.style.dto.HairColorsResponse;
import com.heddy.adapter.in.web.style.dto.StylePreferencesRequest;
import com.heddy.adapter.in.web.style.dto.StylePreferencesResponse;
import com.heddy.adapter.in.web.style.dto.StyleTagsResponse;
import com.heddy.domain.style.model.StyleTagCategory;
import com.heddy.domain.style.port.in.StyleUseCase;
import com.heddy.global.docs.ApiDocs;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "스타일", description = "스타일 태그와 내 선호·제외 스타일 관리")
@SecurityRequirement(name = "bearerAuth")
public class StyleController {

    private final StyleUseCase styleUseCase;

    @GetMapping("/style-tags")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @Operation(summary = "스타일 태그 조회", description = "카테고리를 생략하면 전체 태그를 조회합니다.")
    public ApiResponse<StyleTagsResponse> getStyleTags(
            @Parameter(description = "태그 카테고리. 생략하면 전체 태그를 돌려준다")
            @RequestParam(required = false) StyleTagCategory category,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                StyleTagsResponse.from(styleUseCase.getStyleTags(category)),
                RequestIdFilter.get(servletRequest));
    }

    @GetMapping("/hair-colors")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @Operation(summary = "헤어 컬러 팔레트 조회",
            description = "후보 스타일 저장·추천 응답·AR 이 함께 쓰는 색상 카탈로그입니다. "
                    + "노출 순서대로 정렬되며, 더 쓰지 않는 색은 목록에서 빠집니다 "
                    + "(이미 저장된 후보가 가리키는 색은 그대로 남습니다).")
    public ApiResponse<HairColorsResponse> getHairColors(HttpServletRequest servletRequest) {
        return ApiResponse.success(
                HairColorsResponse.from(styleUseCase.getHairColors()),
                RequestIdFilter.get(servletRequest));
    }

    @GetMapping("/me/style-preferences")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @Operation(summary = "내 선호·제외 스타일 태그 조회")
    public ApiResponse<StylePreferencesResponse> getStylePreferences(
            @AuthenticationPrincipal UUID userId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                StylePreferencesResponse.from(styleUseCase.getStylePreferences(userId)),
                RequestIdFilter.get(servletRequest));
    }

    @PutMapping("/me/style-preferences")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @ApiDocs.Validated
    @ApiDocs.StylePreference
    @Operation(summary = "내 선호·제외 스타일 태그 전체 저장")
    public ApiResponse<StylePreferencesResponse> saveStylePreferences(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody StylePreferencesRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                StylePreferencesResponse.from(
                        styleUseCase.saveStylePreferences(request.toCommand(userId))),
                RequestIdFilter.get(servletRequest));
    }
}
