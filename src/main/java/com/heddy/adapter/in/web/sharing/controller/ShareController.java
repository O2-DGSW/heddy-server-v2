package com.heddy.adapter.in.web.sharing.controller;

import com.heddy.adapter.in.web.sharing.dto.CreateShareRequest;
import com.heddy.adapter.in.web.sharing.dto.ShareResponse;
import com.heddy.adapter.in.web.sharing.dto.ShareSummaryResponse;
import com.heddy.domain.sharing.model.ShareStatus;
import com.heddy.domain.sharing.port.in.CreateShareUseCase;
import com.heddy.domain.sharing.port.in.ListSharesUseCase;
import com.heddy.global.filter.RequestIdFilter;
import com.heddy.global.response.ApiResponse;
import com.heddy.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "공유", description = "공유 링크 생성·목록 조회")
@SecurityRequirement(name = "bearerAuth")
public class ShareController {

    private final CreateShareUseCase createShareUseCase;
    private final ListSharesUseCase listSharesUseCase;

    @PostMapping("/shares")
    @Operation(summary = "공유 링크 생성",
            description = "기록 또는 후보 스타일 1개 이상과 노출 항목 1개 이상이 필요하다. 모든 대상은 "
                    + "본인 소유여야 하고, 남의 기록은 존재 여부를 드러내지 않게 404 로 답한다. 토큰 원문은 "
                    + "응답의 share_url 로 딱 한 번 내려가고 서버에는 해시만 저장된다.")
    public ResponseEntity<ApiResponse<ShareResponse>> create(
            @AuthenticationPrincipal UUID userId,
            @RequestBody CreateShareRequest request,
            HttpServletRequest servletRequest
    ) {
        // 새 리소스를 만드는 API 라 201 로 답한다.
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                ShareResponse.from(createShareUseCase.create(request.toCommand(userId))),
                RequestIdFilter.get(servletRequest)));
    }

    @GetMapping("/shares")
    @Operation(summary = "내 공유 목록 조회",
            description = "내 공유를 최신순으로 페이지 조회합니다. 상태 필터는 생략할 수 있고, "
                    + "결과가 없으면 200과 빈 items를 반환합니다. share_url 은 저장된 것이 없어 "
                    + "생성 응답으로만 내려간다.")
    public ApiResponse<PageResponse<ShareSummaryResponse>> list(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "상태 필터(ACTIVE/REVOKED). 생략 시 전체")
            @RequestParam(required = false) ShareStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest servletRequest
    ) {
        ListSharesUseCase.Result result = listSharesUseCase.list(
                new ListSharesUseCase.Query(userId, status, page, size));
        PageResponse<ShareSummaryResponse> response = PageResponse.of(
                result.items().stream().map(ShareSummaryResponse::from).toList(),
                result.page(), result.size(), result.totalElements());
        return ApiResponse.success(response, RequestIdFilter.get(servletRequest));
    }
}
