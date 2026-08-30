package com.heddy.domain.analysis.port.out;

import com.heddy.domain.analysis.model.AnalysisJobStatus;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * 주어진 시술기록들의 최신 분석 상태를 한 번에 묻는다. 목록의 분석 배지가 유일한 소비자다.
 *
 * <p>기록 하나씩 묻지 않고 집합으로 받는 이유는 목록이 페이지 단위이기 때문이다 — 기록마다
 * 질의하면 페이지 크기만큼 왕복이 늘어난다(공유 여부 조회와 같은 이유).
 *
 * <p>"최신"은 기록당 가장 최근 접수된 작업 한 건이다. 과거 작업은 그때 사진의 결과로 확정된
 * 이력이라 배지가 대표하지 않는다.
 */
@FunctionalInterface
public interface LatestAnalysisStatusPort {

    /**
     * @param recordIds 조회할 기록들. 비어 있으면 질의 없이 빈 map 이다
     * @return 기록 ID → 최신 분석 상태. 분석을 한 번도 요청하지 않은 기록은 담기지 않는다
     */
    Map<UUID, AnalysisJobStatus> findLatestStatuses(Collection<UUID> recordIds);
}
