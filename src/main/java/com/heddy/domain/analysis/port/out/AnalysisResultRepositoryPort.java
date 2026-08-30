package com.heddy.domain.analysis.port.out;

import com.heddy.domain.analysis.model.AnalysisResult;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisResultRepositoryPort {

    /** 콜백이 가져온 결과를 저장한다. 작업 하나에 결과는 하나다. */
    AnalysisResult insert(AnalysisResult result);

    /**
     * 소유자 조건까지 걸어 조회한다. 남의 결과는 없는 결과와 같은 404 이고, 질의 횟수도 같아야
     * 존재 여부가 새지 않는다(시술기록 #31 컨벤션).
     */
    Optional<AnalysisResult> findByIdAndUserId(UUID analysisId, UUID userId);

    /** 기록의 가장 최근 결과. `GET /treatment-records/{recordId}/analyses/latest` 가 쓴다. */
    Optional<AnalysisResult> findLatestByRecordId(UUID recordId);

    /** 작업이 이미 결과를 낸 적 있는지. 콜백 멱등 처리가 이걸로 중복 저장을 거른다. */
    Optional<AnalysisResult> findByJobId(UUID jobId);
}
