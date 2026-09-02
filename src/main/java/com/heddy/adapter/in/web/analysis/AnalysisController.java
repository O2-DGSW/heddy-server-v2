package com.heddy.adapter.in.web.analysis;

import com.heddy.adapter.in.web.analysis.dto.AnalysisResponse;
import com.heddy.domain.analysis.port.in.GetLatestAnalysisUseCase;
import com.heddy.global.docs.ApiDocs;
import com.heddy.global.filter.RequestIdFilter;
import com.heddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "AI 분석", description = "시술기록의 모발 분석 결과 조회")
@SecurityRequirement(name = "bearerAuth")
public class AnalysisController {

    private final GetLatestAnalysisUseCase getLatestAnalysisUseCase;

    @GetMapping("/treatment-records/{recordId}/analyses/latest")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @ApiDocs.OwnedResource
    @Operation(summary = "최신 분석 결과 조회",
            description = "기록의 가장 최근 분석 결과를 돌려준다. 분석한 적이 없으면 404 다. "
                    + "사진이 바뀌어 결과가 현재 사진을 반영하지 않으면 status 가 STALE 이며, "
                    + "이때도 결과는 그대로 내려간다.")
    public ApiResponse<AnalysisResponse> getLatest(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "시술기록 식별자. 남의 기록은 존재 여부를 드러내지 않게 "
                    + "없는 기록과 같은 404 로 답한다", required = true)
            @PathVariable UUID recordId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                AnalysisResponse.from(getLatestAnalysisUseCase.get(
                        new GetLatestAnalysisUseCase.Query(userId, recordId))),
                RequestIdFilter.get(servletRequest));
    }
}
