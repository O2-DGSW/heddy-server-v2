package com.heddy.adapter.in.web.style.controller;

import com.heddy.adapter.in.web.style.dto.SaveStyleRequest;
import com.heddy.adapter.in.web.style.dto.SavedStyleResponse;
import com.heddy.adapter.in.web.style.dto.UpdateSavedStyleRequest;
import com.heddy.domain.style.port.in.SavedStyleUseCase;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/saved-styles")
@Tag(name = "저장한 후보 스타일", description = "AR 로 시연한 스타일을 보관하고 다시 꺼내 쓴다")
@SecurityRequirement(name = "bearerAuth")
public class SavedStyleController {

    private final SavedStyleUseCase savedStyleUseCase;

    @GetMapping
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @Operation(summary = "저장한 후보 스타일 목록",
            description = "최신 저장순으로 돌려줍니다. 이미지 URL 은 저장값이 아니라 조회 시점에 "
                    + "짧은 만료의 Presigned GET 으로 발급합니다.")
    public ApiResponse<PageResponse<SavedStyleResponse>> list(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest servletRequest
    ) {
        SavedStyleUseCase.Page result = savedStyleUseCase.list(userId, page, size);
        return ApiResponse.success(
                PageResponse.of(
                        result.items().stream().map(SavedStyleResponse::from).toList(),
                        page, size, result.totalElements()),
                RequestIdFilter.get(servletRequest));
    }

    @PatchMapping("/{savedStyleId}")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @ApiDocs.Validated
    @ApiDocs.OwnedResource
    @Operation(summary = "후보 스타일 메모 수정",
            description = "memo에 null을 보내면 기존 메모를 삭제합니다.")
    public ApiResponse<SavedStyleResponse> updateMemo(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID savedStyleId,
            @Valid @RequestBody UpdateSavedStyleRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                SavedStyleResponse.from(savedStyleUseCase.updateMemo(
                        request.toCommand(userId, savedStyleId))),
                RequestIdFilter.get(servletRequest));
    }

    @PostMapping
    @ApiDocs.Created
    @ApiDocs.Authenticated
    @ApiDocs.Validated
    @ApiDocs.SavedStyleCreation
    @Operation(summary = "후보 스타일 저장",
            description = "카탈로그에 있는 스타일과 색상만 저장할 수 있습니다. 같은 스타일·색상 "
                    + "조합은 중복 저장할 수 없고 사용자당 최대 20개까지 저장합니다.")
    public ResponseEntity<ApiResponse<SavedStyleResponse>> save(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody SaveStyleRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                SavedStyleResponse.from(savedStyleUseCase.save(request.toCommand(userId))),
                RequestIdFilter.get(servletRequest)));
    }

    @DeleteMapping("/{savedStyleId}")
    @ApiDocs.NoContent
    @ApiDocs.Authenticated
    @ApiDocs.OwnedResource
    @Operation(summary = "후보 스타일 삭제",
            description = "이 후보를 실은 공유에서도 함께 빠집니다. 공유 링크는 살아 있고 사라진 "
                    + "내용만 빠집니다. AR 캡처 파일은 정리 대상으로 표시됩니다.")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "후보 식별자. 남의 후보는 존재 여부를 드러내지 않게 "
                    + "없는 후보와 같은 404 로 답한다")
            @PathVariable UUID savedStyleId
    ) {
        savedStyleUseCase.delete(userId, savedStyleId);
        return ResponseEntity.noContent().build();
    }
}
