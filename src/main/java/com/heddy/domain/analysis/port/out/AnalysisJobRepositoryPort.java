package com.heddy.domain.analysis.port.out;

import com.heddy.domain.analysis.model.AnalysisJob;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisJobRepositoryPort {

    /** 접수된 작업을 저장한다. */
    AnalysisJob insert(AnalysisJob job);

    /** 상태·진행률·실패 사유를 저장한다. 식별자와 대상은 바뀌지 않는다. */
    AnalysisJob update(AnalysisJob job);

    /**
     * 소유자 조건까지 걸어 조회한다. 남의 작업은 없는 작업과 같은 404 이고, 질의 횟수도 같아야
     * 존재 여부가 새지 않는다(시술기록 #31 컨벤션).
     */
    Optional<AnalysisJob> findByIdAndUserId(UUID jobId, UUID userId);

    /** 기록의 가장 최근 작업. 최신 분석 조회와 무효화가 이 한 건을 대상으로 한다. */
    Optional<AnalysisJob> findLatestByRecordId(UUID recordId);

    /** 같은 사진에 진행 중인 작업. 중복 접수를 애플리케이션에서 먼저 걸러 준다. */
    Optional<AnalysisJob> findInProgressByPhotoId(UUID photoId);
}
