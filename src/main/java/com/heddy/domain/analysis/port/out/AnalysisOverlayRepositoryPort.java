package com.heddy.domain.analysis.port.out;

import com.heddy.domain.analysis.model.AnalysisOverlay;

import java.util.List;
import java.util.UUID;

public interface AnalysisOverlayRepositoryPort {

    /** 콜백이 가져온 오버레이를 저장한다. 결과 하나에 종류마다 한 장이다. */
    AnalysisOverlay insert(AnalysisOverlay overlay);

    /** 결과에 딸린 오버레이 전부. 결과 조회가 함께 읽는다. */
    List<AnalysisOverlay> findByAnalysisId(UUID analysisId);
}
