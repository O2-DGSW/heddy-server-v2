package com.heddy.domain.analysis.port.out;

import java.util.UUID;

/** 사진 변경 시 해당 시술기록의 최신 분석을 STALE 상태로 전이하는 연결 지점. */
@FunctionalInterface
public interface AnalysisStalenessPort {

    void markLatestStale(UUID treatmentRecordId);
}
