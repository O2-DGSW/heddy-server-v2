package com.heddy.domain.analysis.model;

/**
 * 분석 지표 4종. 방향을 여기 남기는 이유는 {@code ROUGHNESS} 만 높은 값이 부정적이기
 * 때문이다 — 소비하는 쪽마다 다시 판단하면 언젠가 한 곳이 게이지를 반대로 그린다.
 */
public enum MetricType {
    COLOR_UNIFORMITY(true),
    SHAPE_SYMMETRY(true),
    VOLUME_BALANCE(true),
    ROUGHNESS(false);

    private final boolean higherIsBetter;

    MetricType(boolean higherIsBetter) {
        this.higherIsBetter = higherIsBetter;
    }

    public boolean higherIsBetter() {
        return higherIsBetter;
    }
}
