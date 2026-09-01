package com.heddy.adapter.in.web.recommendation;

import com.heddy.adapter.in.web.recommendation.dto.GenerateRecommendationRequest;
import com.heddy.adapter.in.web.recommendation.dto.RecommendationResponse;
import com.heddy.domain.recommendation.port.in.GenerateRecommendationUseCase;
import com.heddy.domain.recommendation.port.in.GetLatestRecommendationUseCase;
import com.heddy.domain.recommendation.port.in.GetRecommendationUseCase;
import com.heddy.global.filter.RequestIdFilter;
import com.heddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "추천", description = "규칙 기반 헤어스타일 추천 생성·조회")
@SecurityRequirement(name = "bearerAuth")
public class RecommendationController {
    private final GenerateRecommendationUseCase generateUseCase;
    private final GetLatestRecommendationUseCase getLatestUseCase;
    private final GetRecommendationUseCase getUseCase;

    @PostMapping("/recommendations/generate")
    @Operation(summary = "헤어스타일 추천 생성",
            description = "구조화된 사용자 데이터만 사용해 설명 가능한 Top 3를 생성합니다.")
    public ApiResponse<RecommendationResponse> generate(
            @AuthenticationPrincipal UUID userId,
            @RequestBody(required = false) GenerateRecommendationRequest request,
            HttpServletRequest servletRequest
    ) {
        boolean forceRefresh = request != null && request.shouldForceRefresh();
        return ApiResponse.success(RecommendationResponse.from(
                generateUseCase.generate(userId, forceRefresh)), RequestIdFilter.get(servletRequest));
    }

    @GetMapping("/recommendations/latest")
    @Operation(summary = "최신 추천 조회")
    public ApiResponse<RecommendationResponse> latest(
            @AuthenticationPrincipal UUID userId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(RecommendationResponse.from(getLatestUseCase.getLatest(userId)),
                RequestIdFilter.get(servletRequest));
    }

    @GetMapping("/recommendations/{recommendationRunId}")
    @Operation(summary = "추천 실행 단건 조회",
            description = "소유자 조건을 포함해 조회하며 썸네일 URL은 매번 새로 발급합니다.")
    public ApiResponse<RecommendationResponse> get(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID recommendationRunId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(RecommendationResponse.from(
                getUseCase.get(userId, recommendationRunId)), RequestIdFilter.get(servletRequest));
    }
}
