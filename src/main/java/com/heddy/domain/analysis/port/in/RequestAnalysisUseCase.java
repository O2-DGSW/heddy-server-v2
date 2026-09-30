package com.heddy.domain.analysis.port.in;

import com.heddy.domain.analysis.model.AnalysisJob;

import java.util.UUID;

/** 시술기록 사진의 로컬 모델 분석 작업을 접수한다. */
public interface RequestAnalysisUseCase {

    AnalysisJob request(Command command);

    record Command(UUID requesterId, UUID recordId, UUID photoId) {
    }
}
