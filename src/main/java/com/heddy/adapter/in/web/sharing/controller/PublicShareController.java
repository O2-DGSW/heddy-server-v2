package com.heddy.adapter.in.web.sharing.controller;

import com.heddy.adapter.in.web.sharing.dto.PublicShareResponse;
import com.heddy.domain.sharing.port.in.GetPublicShareUseCase;
import com.heddy.global.docs.ApiDocs;
import com.heddy.global.filter.RequestIdFilter;
import com.heddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 무인증 공개 조회. 보안 요건(스펙 19절)을 응답 헤더로 이행한다 — 캐시 금지와 검색엔진
 * 색인 금지는 성공 응답뿐 아니라 만료·철패 오류에도 적용한다(핸들러가 같은 헤더를 단다).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "공개 공유", description = "인증 없이 접근하는 공유 링크 조회")
public class PublicShareController {

    private final GetPublicShareUseCase getPublicShareUseCase;

    @GetMapping("/public/shares/{shareToken}")
    @ApiDocs.Ok
    @ApiDocs.PublicShare
    @Operation(summary = "공개 공유 조회",
            description = "토큰 원문으로 해시를 대조하고 철회·만료를 매 요청 검증합니다. "
                    + "공유에서 선택하지 않은 항목은 응답 키 자체가 없습니다.")
    public ResponseEntity<ApiResponse<PublicShareResponse>> get(
            @Parameter(description = "공유 링크의 토큰 원문. 서버는 해시만 저장하므로 이 값이 "
                    + "링크에 접근할 수 있는 유일한 열쇠다. 철회된 링크는 SHARE_REVOKED, "
                    + "만료된 링크는 SHARE_EXPIRED 로 구분해 답한다", required = true)
            @PathVariable String shareToken,
            HttpServletRequest servletRequest
    ) {
        GetPublicShareUseCase.Result result = getPublicShareUseCase.get(
                new GetPublicShareUseCase.Query(shareToken));
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(ApiResponse.success(PublicShareResponse.from(result),
                        RequestIdFilter.get(servletRequest)));
    }
}
