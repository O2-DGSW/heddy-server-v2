package com.heddy.domain.analysis.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 분석 결과에 딸린 오버레이 이미지 한 장.
 *
 * <p>URL 이 아니라 {@code fileId} 를 담는다. 오버레이 PNG 는 비공개 버킷에 있고 조회 때 짧은
 * 만료 URL 을 발급받는다 — 저장된 URL 은 만료된 뒤에도 행에 남아 언제든 새는 값이 된다
 * (시술기록 사진과 같은 규칙).
 *
 * <p>결과({@link AnalysisResult})와 같은 애그리게이트로 묶지 않았다. 결과는 콜백 한 번에
 * 확정되고 그 뒤로 바뀌지 않는 값인 반면 오버레이는 종류가 늘 수 있어, 한 덩어리로 두면 종류가
 * 늘 때마다 결과 모델과 행을 함께 건드리게 된다.
 */
public record AnalysisOverlay(
        UUID overlayId,
        UUID analysisId,
        OverlayType overlayType,
        UUID fileId,
        Instant createdAt
) {
    public AnalysisOverlay {
        Objects.requireNonNull(overlayId, "overlayId");
        Objects.requireNonNull(analysisId, "analysisId");
        Objects.requireNonNull(overlayType, "overlayType");
        Objects.requireNonNull(fileId, "fileId");
    }

    public static AnalysisOverlay create(
            UUID analysisId, OverlayType overlayType, UUID fileId, Instant now) {
        return new AnalysisOverlay(UUID.randomUUID(), analysisId, overlayType, fileId, now);
    }

    /** 이미 읽어 온 행을 도메인으로 되돌릴 때 쓴다. */
    public static AnalysisOverlay reconstitute(
            UUID overlayId, UUID analysisId, OverlayType overlayType, UUID fileId,
            Instant createdAt) {
        return new AnalysisOverlay(overlayId, analysisId, overlayType, fileId, createdAt);
    }
}
