package com.heddy.adapter.in.web.summary.controller;

import com.heddy.adapter.in.web.summary.dto.MySummaryResponse;
import com.heddy.domain.summary.port.in.GetMySummaryUseCase;
import com.heddy.global.docs.ApiDocs;
import com.heddy.global.filter.RequestIdFilter;
import com.heddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/me/summary")
@RequiredArgsConstructor
@Tag(name = "홈 요약", description = "홈 화면 상단 요약 타일의 카운트")
@SecurityRequirement(name = "bearerAuth")
public class SummaryController {

    private final GetMySummaryUseCase getMySummaryUseCase;

    @GetMapping
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @Operation(summary = "홈 요약 카운트 조회",
            description = "시술 기록·분석 완료 기록·후보 스타일·공유 중 기록 수를 한 번에 "
                    + "돌려줍니다. shared_record_count 는 공유 링크 수가 아니라 살아있는 "
                    + "공유에 포함된 시술 기록 수입니다 — 링크 하나가 기록 여러 개를 담을 수 "
                    + "있고 같은 기록을 여러 링크로 공유할 수도 있어 두 숫자는 단위가 다릅니다. "
                    + "자료가 없으면 0 입니다.")
    public ApiResponse<MySummaryResponse> getSummary(
            @AuthenticationPrincipal UUID userId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                MySummaryResponse.from(getMySummaryUseCase.get(userId)),
                RequestIdFilter.get(servletRequest));
    }
}
