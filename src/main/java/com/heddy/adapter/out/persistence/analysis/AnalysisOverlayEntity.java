package com.heddy.adapter.out.persistence.analysis;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.analysis.exception.AnalysisError;
import com.heddy.domain.analysis.exception.AnalysisException;
import com.heddy.domain.analysis.model.AnalysisOverlay;
import com.heddy.domain.analysis.model.OverlayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** 오버레이의 JPA 표현. 종류를 이름으로 저장하는 이유는 작업·결과와 같다. */
@Entity
@Table(name = "analysis_overlays")
class AnalysisOverlayEntity extends BaseEntity {

    @Id
    @Column(name = "overlay_id", nullable = false, updatable = false)
    private UUID overlayId;

    @Column(name = "analysis_id", nullable = false, updatable = false)
    private UUID analysisId;

    @Column(name = "overlay_type", nullable = false, updatable = false, length = 30)
    private String overlayType;

    @Column(name = "file_id", nullable = false, updatable = false)
    private UUID fileId;

    protected AnalysisOverlayEntity() {
    }

    AnalysisOverlayEntity(AnalysisOverlay overlay) {
        overlayId = overlay.overlayId();
        analysisId = overlay.analysisId();
        overlayType = overlay.overlayType().name();
        fileId = overlay.fileId();
    }

    AnalysisOverlay toDomain() {
        return AnalysisOverlay.reconstitute(
                overlayId, analysisId, parseType(), fileId, getCreatedAt());
    }

    private OverlayType parseType() {
        try {
            return OverlayType.valueOf(overlayType);
        } catch (IllegalArgumentException invalidName) {
            throw new AnalysisException(AnalysisError.OVERLAY_TYPE_UNKNOWN);
        }
    }
}
