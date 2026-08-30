package com.heddy.domain.analysis.model;

import com.heddy.domain.analysis.exception.AnalysisError;
import com.heddy.domain.analysis.exception.AnalysisException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisResultTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID PHOTO_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-30T10:00:00Z");

    @Test
    void carriesTheJobsOwnerAndSubjectSoItCanBeReadBackAlone() {
        AnalysisJob job = AnalysisJob.create(USER_ID, RECORD_ID, PHOTO_ID, NOW);

        AnalysisResult result = AnalysisResult.create(job, metrics(),
                MetricScore.of("82.40", ConfidenceGrade.HIGH), "hair-v1.2.0",
                "사진에서 거칠게 보이는 영역이 감지되었습니다", null, NOW);

        assertThat(result.jobId()).isEqualTo(job.jobId());
        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.recordId()).isEqualTo(RECORD_ID);
        assertThat(result.photoId()).isEqualTo(PHOTO_ID);
    }

    /** 소수점이 깎이면 비교 분석의 Δ값이 그만큼 어긋난다. */
    @Test
    void keepsScoresToTwoDecimalPlaces() {
        AnalysisResult result = result(metrics());

        assertThat(result.metric(MetricType.COLOR_UNIFORMITY).score())
                .isEqualByComparingTo(new BigDecimal("82.40"));
    }

    @Test
    void refusesAResultMissingAnyOfTheFourMetrics() {
        Map<MetricType, MetricScore> partial = new EnumMap<>(MetricType.class);
        partial.put(MetricType.COLOR_UNIFORMITY, MetricScore.of("82.40", ConfidenceGrade.HIGH));

        assertThatThrownBy(() -> result(partial))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.RESULT_METRICS_INCOMPLETE);
    }

    /** 어느 모델이 낸 점수인지 모르면 과거 결과와 비교하는 기능이 성립하지 않는다. */
    @Test
    void refusesAResultWithoutAModelVersion() {
        assertThatThrownBy(() -> AnalysisResult.reconstitute(
                UUID.randomUUID(), UUID.randomUUID(), USER_ID, RECORD_ID, PHOTO_ID, metrics(),
                MetricScore.of("82.40", ConfidenceGrade.HIGH), "  ", null, null, NOW))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error",
                        AnalysisError.RESULT_MODEL_VERSION_REQUIRED);
    }

    @Test
    void refusesScoresOutsideZeroToHundred() {
        assertThatThrownBy(() -> MetricScore.of("100.01", ConfidenceGrade.HIGH))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.RESULT_SCORE_INVALID);
        assertThatThrownBy(() -> MetricScore.of("-0.01", ConfidenceGrade.HIGH))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.RESULT_SCORE_INVALID);
    }

    /** 신뢰도가 낮은 지표는 두 결과의 차를 내도 의미가 없다. */
    @Test
    void leavesLowConfidenceMetricsOutOfComparison() {
        Map<MetricType, MetricScore> mixed = metrics();
        mixed.put(MetricType.ROUGHNESS, MetricScore.of("71.00", ConfidenceGrade.LOW));

        assertThat(result(mixed).comparableMetrics()).doesNotContainKey(MetricType.ROUGHNESS)
                .containsKeys(MetricType.COLOR_UNIFORMITY, MetricType.SHAPE_SYMMETRY,
                        MetricType.VOLUME_BALANCE);
    }

    /** roughness 만 높은 값이 부정적이다. 방향을 도메인이 들고 있어야 게이지가 뒤집히지 않는다. */
    @Test
    void marksRoughnessAsTheOnlyMetricWhereHigherIsWorse() {
        assertThat(MetricType.ROUGHNESS.higherIsBetter()).isFalse();
        assertThat(MetricType.COLOR_UNIFORMITY.higherIsBetter()).isTrue();
        assertThat(MetricType.SHAPE_SYMMETRY.higherIsBetter()).isTrue();
        assertThat(MetricType.VOLUME_BALANCE.higherIsBetter()).isTrue();
    }

    private AnalysisResult result(Map<MetricType, MetricScore> metrics) {
        return AnalysisResult.reconstitute(UUID.randomUUID(), UUID.randomUUID(), USER_ID,
                RECORD_ID, PHOTO_ID, metrics, MetricScore.of("82.40", ConfidenceGrade.HIGH),
                "hair-v1.2.0", null, null, NOW);
    }

    private Map<MetricType, MetricScore> metrics() {
        Map<MetricType, MetricScore> metrics = new EnumMap<>(MetricType.class);
        metrics.put(MetricType.COLOR_UNIFORMITY, MetricScore.of("82.40", ConfidenceGrade.HIGH));
        metrics.put(MetricType.SHAPE_SYMMETRY, MetricScore.of("76.20", ConfidenceGrade.MEDIUM));
        metrics.put(MetricType.VOLUME_BALANCE, MetricScore.of("71.00", ConfidenceGrade.MEDIUM));
        metrics.put(MetricType.ROUGHNESS, MetricScore.of("34.50", ConfidenceGrade.HIGH));
        return metrics;
    }
}
