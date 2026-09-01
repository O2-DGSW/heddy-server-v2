package com.heddy.domain.analysis.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 분석 작업의 생애 상태 6종(스펙 AI 분석 절). 상태머신은 서버가 소유한다 — AI 서버는 결과만
 * 돌려주고, 어떤 전이가 유효한지는 여기서만 판단한다.
 *
 * <p>{@code STALE} 은 실패한 작업으로는 갈 수 없다. 무효화란 "결과가 더 이상 현재 사진을
 * 반영하지 않는다"는 뜻인데, {@code FAILED}·{@code UNAVAILABLE} 에는 무효화할 결과가 없다.
 * 반대로 아직 끝나지 않은 작업은 무효화 대상이다 — 사진이 바뀐 뒤에 도착할 결과도 옛 사진의
 * 것이고, 진행 중 자리를 비워 줘야 새 분석을 걸 수 있다.
 */
public enum AnalysisJobStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    UNAVAILABLE,
    STALE;

    private static final Map<AnalysisJobStatus, Set<AnalysisJobStatus>> ALLOWED =
            new EnumMap<>(AnalysisJobStatus.class);

    static {
        ALLOWED.put(PENDING, Set.of(PROCESSING, FAILED, STALE));
        ALLOWED.put(PROCESSING, Set.of(SUCCEEDED, FAILED, UNAVAILABLE, STALE));
        ALLOWED.put(SUCCEEDED, Set.of(STALE));
        ALLOWED.put(FAILED, Set.of());
        ALLOWED.put(UNAVAILABLE, Set.of());
        ALLOWED.put(STALE, Set.of());
    }

    public boolean canMoveTo(AnalysisJobStatus next) {
        return ALLOWED.get(this).contains(next);
    }

    /** 아직 끝나지 않은 작업인지. 같은 사진에 진행 중인 작업을 하나로 묶는 기준이다. */
    public boolean isInProgress() {
        return this == PENDING || this == PROCESSING;
    }

    /** 사유 코드 없이는 성립하지 않는 상태인지. 실패와 재촬영 안내가 여기 해당한다. */
    public boolean requiresFailureReason() {
        return this == FAILED || this == UNAVAILABLE;
    }
}
