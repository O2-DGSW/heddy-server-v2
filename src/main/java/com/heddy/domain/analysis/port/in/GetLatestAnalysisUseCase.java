package com.heddy.domain.analysis.port.in;

import com.heddy.domain.analysis.model.AnalysisJobStatus;
import com.heddy.domain.analysis.model.AnalysisOverlay;
import com.heddy.domain.analysis.model.AnalysisResult;

import java.util.List;
import java.util.UUID;

/** 시술기록의 최신 분석 결과를 조회한다. 상세 화면의 분석 탭이 부른다. */
public interface GetLatestAnalysisUseCase {

    Result get(Query query);

    record Query(UUID requesterId, UUID recordId) {
    }

    /**
     * @param status 결과를 낸 작업의 상태. 사진이 바뀐 뒤라면 {@code STALE} 이고, 이때도 결과는
     *               그대로 내려간다 — 옛 사진의 결과라는 사실은 상태로 알린다
     */
    record Result(AnalysisResult analysis, AnalysisJobStatus status,
                  List<AnalysisOverlay> overlays) {
        public Result {
            overlays = List.copyOf(overlays);
        }
    }
}
