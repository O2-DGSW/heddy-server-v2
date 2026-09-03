package com.heddy.adapter.in.web.style.controller;

import com.heddy.adapter.in.web.style.dto.StylePreferencesRequest;
import com.heddy.adapter.in.web.style.dto.StylePreferencesResponse;
import com.heddy.adapter.in.web.style.dto.StyleTagsResponse;
import com.heddy.adapter.in.web.style.dto.CreateSavedStyleRequest;
import com.heddy.adapter.in.web.style.dto.SavedStyleResponse;
import com.heddy.adapter.in.web.style.dto.UpdateSavedStyleRequest;
import com.heddy.domain.style.model.StyleTagCategory;
import com.heddy.domain.style.port.in.SavedStyleUseCase;
import com.heddy.domain.style.port.in.StyleUseCase;
import com.heddy.global.docs.ApiDocs;
import com.heddy.global.filter.RequestIdFilter;
import com.heddy.global.response.ApiResponse;
import com.heddy.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "스타일", description = "스타일 태그와 내 선호·제외 스타일 관리")
@SecurityRequirement(name = "bearerAuth")
public class StyleController {

    private final StyleUseCase styleUseCase;
    private final SavedStyleUseCase savedStyleUseCase;

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

    @PostMapping("/saved-styles")
    @ApiDocs.Created
    @ApiDocs.Authenticated
    @ApiDocs.Validated
    @ApiDocs.SavedStyleCreation
    @Operation(summary = "후보 스타일 저장",
            description = "추천 결과의 이름·이미지·이유를 스냅샷으로 저장합니다. 사용자당 최대 20개입니다.")
    public ResponseEntity<ApiResponse<SavedStyleResponse>> createSavedStyle(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateSavedStyleRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                SavedStyleResponse.from(savedStyleUseCase.create(request.toCommand(userId))),
                RequestIdFilter.get(servletRequest)));
    }

    @GetMapping("/saved-styles")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @ApiDocs.ListQuery
    @Operation(summary = "내 후보 스타일 목록 조회")
    public ApiResponse<PageResponse<SavedStyleResponse>> getSavedStyles(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest servletRequest
    ) {
        var result = savedStyleUseCase.list(new SavedStyleUseCase.ListQuery(userId, page, size));
        return ApiResponse.success(PageResponse.of(
                        result.items().stream().map(SavedStyleResponse::from).toList(),
                        page, size, result.totalElements()),
                RequestIdFilter.get(servletRequest));
    }

    @PatchMapping("/saved-styles/{savedStyleId}")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @ApiDocs.Validated
    @ApiDocs.OwnedResource
    @Operation(summary = "후보 스타일 메모 수정",
            description = "memo에 null을 보내면 기존 메모를 삭제합니다.")
    public ApiResponse<SavedStyleResponse> updateSavedStyle(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID savedStyleId,
            @Valid @RequestBody UpdateSavedStyleRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(SavedStyleResponse.from(
                        savedStyleUseCase.updateMemo(request.toCommand(userId, savedStyleId))),
                RequestIdFilter.get(servletRequest));
    }

    @DeleteMapping("/saved-styles/{savedStyleId}")
    @ApiDocs.NoContent
    @ApiDocs.Authenticated
    @ApiDocs.OwnedResource
    @Operation(summary = "후보 스타일 삭제")
    public ResponseEntity<Void> deleteSavedStyle(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID savedStyleId
    ) {
        savedStyleUseCase.delete(new SavedStyleUseCase.DeleteCommand(userId, savedStyleId));
        return ResponseEntity.noContent().build();
    }
}
