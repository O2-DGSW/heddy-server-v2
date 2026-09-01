package com.heddy.adapter.in.web.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.analysis.model.AnalysisOverlay;
import com.heddy.domain.analysis.model.MetricType;
import com.heddy.domain.analysis.port.in.GetLatestAnalysisUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 시술기록 상세의 분석 탭이 읽는 응답. */
@Schema(description = "최신 분석 결과")
public record AnalysisResponse(
        @Schema(description = "분석 결과 식별자")
        @JsonProperty("analysis_id") UUID analysisId,

        @Schema(description = "결과를 낸 분석 작업 식별자")
        @JsonProperty("job_id") UUID jobId,

        @Schema(description = "작업 상태. 사진이 바뀐 뒤라면 STALE 이며, 이때도 결과는 그대로 "
                + "내려간다 — 옛 사진의 결과라는 사실은 이 값으로 판단한다",
                allowableValues = {"SUCCEEDED", "STALE"})
        String status,

        @Schema(description = "지표 4종. 화면의 상태 지표 목록이 이 배열을 그린다")
        List<Metric> metrics,

        @Schema(description = "결과 전체 신뢰도")
        Metric confidence,

        @Schema(description = "점수를 낸 모델 버전. 모델이 다르면 과거 결과와 점수를 견줄 수 없다")
        @JsonProperty("model_version") String modelVersion,

        @Schema(description = "결과 요약 문장. 없을 수 있다")
        String summary,

        @Schema(description = "분석이 끝난 시각")
        @JsonProperty("analyzed_at") Instant analyzedAt,

        @Schema(description = "오버레이 이미지 목록. 분석 서버가 만들기 전에는 빈 배열이다")
        List<Overlay> overlays
) {

    @Schema(name = "AnalysisMetric")
    public record Metric(
            @Schema(description = "지표 종류. confidence 는 type 이 비어 있다",
                    allowableValues = {"COLOR_UNIFORMITY", "SHAPE_SYMMETRY", "VOLUME_BALANCE",
                            "ROUGHNESS"})
            String type,

            @Schema(description = "점수. 0~100 이며 소수점 둘째 자리까지 내려간다")
            BigDecimal score,

            @Schema(description = "신뢰도 등급", allowableValues = {"LOW", "MEDIUM", "HIGH"})
            String grade,

            @Schema(description = "높은 값이 좋은 지표인지. ROUGHNESS 만 false 다 — 이 값을 "
                    + "보지 않고 지표 이름으로 분기하면 지표가 늘 때마다 앱을 고쳐야 한다")
            @JsonProperty("higher_is_better") boolean higherIsBetter
    ) {
    }

    @Schema(name = "AnalysisOverlay")
    public record Overlay(
            @Schema(description = "오버레이 종류",
                    allowableValues = {"HAIR_MASK", "COLOR_DIFFERENCE", "VOLUME_GUIDE"})
            String type,

            @Schema(description = "오버레이 이미지 파일 식별자")
            @JsonProperty("file_id") UUID fileId
    ) {
    }

    public static AnalysisResponse from(GetLatestAnalysisUseCase.Result result) {
        var analysis = result.analysis();
        return new AnalysisResponse(
                analysis.analysisId(), analysis.jobId(), result.status().name(),
                metrics(result), confidence(result), analysis.modelVersion(), analysis.summary(),
                analysis.analyzedAt(), overlays(result.overlays()));
    }

    private static List<Metric> metrics(GetLatestAnalysisUseCase.Result result) {
        // 지표 순서를 열거형 선언 순서로 고정한다. map 순회 순서에 맡기면 화면의 목록 순서가
        // 요청마다 달라진다.
        return java.util.Arrays.stream(MetricType.values())
                .map(type -> {
                    var metric = result.analysis().metric(type);
                    return new Metric(type.name(), metric.score(), metric.grade().name(),
                            type.higherIsBetter());
                })
                .toList();
    }

    private static Metric confidence(GetLatestAnalysisUseCase.Result result) {
        var confidence = result.analysis().confidence();
        return new Metric(null, confidence.score(), confidence.grade().name(), true);
    }

    private static List<Overlay> overlays(List<AnalysisOverlay> overlays) {
        return overlays.stream()
                .map(overlay -> new Overlay(overlay.overlayType().name(), overlay.fileId()))
                .toList();
    }
}
