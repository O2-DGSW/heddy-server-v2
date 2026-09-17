package com.heddy.domain.analysis.port.in;

import com.heddy.domain.analysis.model.AnalysisJob;

import java.util.UUID;

/** 시스템 실패로 끝난 작업을 새 작업으로 재접수한다. */
public interface RetryAnalysisUseCase {

    AnalysisJob retry(Command command);

    record Command(UUID requesterId, UUID jobId) {
    }
}
