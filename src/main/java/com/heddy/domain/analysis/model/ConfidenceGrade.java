package com.heddy.domain.analysis.model;

/**
 * 지표별 신뢰도 등급. 점수에서 계산하지 않고 AI 서버가 준 값을 그대로 담는다 — 스펙의 등급
 * 기준표(90~100=HIGH)와 응답 예시(82.4=HIGH)가 서로 맞지 않아, 계산식을 도메인이 고르면 어느
 * 쪽이든 스펙과 어긋난다.
 */
public enum ConfidenceGrade {
    LOW,
    MEDIUM,
    HIGH;

    /** 비교 분석에서 이 지표를 견줄 수 있는지. 신뢰도가 낮은 지표는 comparable=false 다. */
    public boolean isComparable() {
        return this != LOW;
    }
}
