package com.heddy.domain.analysis.model;

import com.heddy.domain.analysis.exception.AnalysisError;
import com.heddy.domain.analysis.exception.AnalysisException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 지표 하나의 점수와 신뢰도 등급. 신뢰도(confidence) 자체도 같은 모양이라 함께 쓴다.
 *
 * <p>{@code double} 이 아니라 {@link BigDecimal} 이다. 점수는 소수점 둘째 자리까지 저장되고
 * 비교 분석이 두 결과의 차를 내므로, 이진 부동소수점의 오차가 그대로 Δ값에 실린다.
 */
public record MetricScore(BigDecimal score, ConfidenceGrade grade) {

    private static final BigDecimal MIN = BigDecimal.ZERO;
    private static final BigDecimal MAX = BigDecimal.valueOf(100);

    public MetricScore {
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(grade, "grade");
        if (score.compareTo(MIN) < 0 || score.compareTo(MAX) > 0) {
            throw new AnalysisException(AnalysisError.RESULT_SCORE_INVALID);
        }
    }

    public static MetricScore of(String score, ConfidenceGrade grade) {
        return new MetricScore(new BigDecimal(score), grade);
    }
}
