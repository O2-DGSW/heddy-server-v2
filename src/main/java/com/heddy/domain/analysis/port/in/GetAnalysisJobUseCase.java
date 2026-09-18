package com.heddy.domain.analysis.port.in;

import com.heddy.domain.analysis.model.AnalysisJob;

import java.util.UUID;

/** 비동기 분석 작업 상태를 조회한다. */
public interface GetAnalysisJobUseCase {

    Result get(Query query);

    record Query(UUID requesterId, UUID jobId) {
    }

    record Result(AnalysisJob job, UUID analysisId) {
    }
}
