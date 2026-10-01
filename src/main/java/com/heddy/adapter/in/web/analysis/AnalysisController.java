package com.heddy.adapter.in.web.analysis;

import com.heddy.adapter.in.web.analysis.dto.AnalysisResponse;
import com.heddy.adapter.in.web.analysis.dto.AnalysisJobAcceptedResponse;
import com.heddy.adapter.in.web.analysis.dto.AnalysisJobResponse;
import com.heddy.adapter.in.web.analysis.dto.RequestAnalysisRequest;
import com.heddy.domain.analysis.port.in.GetAnalysisJobUseCase;
import com.heddy.domain.analysis.port.in.GetAnalysisUseCase;
import com.heddy.domain.analysis.port.in.GetLatestAnalysisUseCase;
import com.heddy.domain.analysis.port.in.RequestAnalysisUseCase;
import com.heddy.domain.analysis.port.in.RetryAnalysisUseCase;
import com.heddy.global.docs.ApiDocs;
import com.heddy.global.filter.RequestIdFilter;
import com.heddy.global.response.ApiResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "AI 분석", description = "시술기록의 모발 분석 결과 조회")
@SecurityRequirement(name = "bearerAuth")
public class AnalysisController {

    private final GetLatestAnalysisUseCase getLatestAnalysisUseCase;
    private final RequestAnalysisUseCase requestAnalysisUseCase;
    private final GetAnalysisJobUseCase getAnalysisJobUseCase;
    private final RetryAnalysisUseCase retryAnalysisUseCase;
    private final GetAnalysisUseCase getAnalysisUseCase;

    @PostMapping("/treatment-records/{recordId}/analyses")
    @ApiDocs.Accepted
    @ApiDocs.Authenticated
    @ApiDocs.OwnedResource
    @Operation(summary = "머리 분석 작업 요청",
            description = "S3의 READY 시술 사진을 실제 SegFormer ONNX 모델로 비동기 분석한다. "
                    + "모델을 사용할 수 없으면 임의 점수를 만들지 않고 503으로 답한다.")
    public ResponseEntity<ApiResponse<AnalysisJobAcceptedResponse>> request(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID recordId,
            @RequestBody(required = false) RequestAnalysisRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID photoId = request == null ? null : request.photoId();
        var job = requestAnalysisUseCase.request(
                new RequestAnalysisUseCase.Command(userId, recordId, photoId));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(
                AnalysisJobAcceptedResponse.from(job), RequestIdFilter.get(servletRequest)));
    }

    @GetMapping("/analysis-jobs/{jobId}")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @ApiDocs.OwnedResource
    @Operation(summary = "분석 작업 상태 조회")
    public ApiResponse<AnalysisJobResponse> getJob(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID jobId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(AnalysisJobResponse.from(getAnalysisJobUseCase.get(
                        new GetAnalysisJobUseCase.Query(userId, jobId))),
                RequestIdFilter.get(servletRequest));
    }

    @PostMapping("/analysis-jobs/{jobId}/retry")
    @ApiDocs.Accepted
    @ApiDocs.Authenticated
    @ApiDocs.OwnedResource
    @Operation(summary = "실패한 분석 작업 재시도",
            description = "FAILED 작업만 새 작업 ID로 재접수한다. UNAVAILABLE은 재촬영 대상이다.")
    public ResponseEntity<ApiResponse<AnalysisJobAcceptedResponse>> retry(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID jobId,
            HttpServletRequest servletRequest
    ) {
        var job = retryAnalysisUseCase.retry(new RetryAnalysisUseCase.Command(userId, jobId));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(
                AnalysisJobAcceptedResponse.from(job), RequestIdFilter.get(servletRequest)));
    }

    @GetMapping("/analyses/{analysisId}")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @ApiDocs.OwnedResource
    @Operation(summary = "분석 결과 조회")
    public ApiResponse<AnalysisResponse> getAnalysis(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID analysisId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(AnalysisResponse.from(getAnalysisUseCase.get(
                        new GetAnalysisUseCase.Query(userId, analysisId))),
                RequestIdFilter.get(servletRequest));
    }

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
