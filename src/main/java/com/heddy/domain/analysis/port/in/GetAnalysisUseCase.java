package com.heddy.domain.analysis.port.in;

import com.heddy.domain.analysis.model.AnalysisJobStatus;
import com.heddy.domain.analysis.model.AnalysisOverlay;
import com.heddy.domain.analysis.model.AnalysisResult;

import java.util.List;
import java.util.UUID;

/** 분석 결과 식별자로 결과를 조회한다. */
public interface GetAnalysisUseCase {

    Result get(Query query);

    record Query(UUID requesterId, UUID analysisId) {
    }

    record Result(AnalysisResult analysis, AnalysisJobStatus status,
                  List<AnalysisOverlay> overlays) {
        public Result {
            overlays = List.copyOf(overlays);
        }
    }
}
