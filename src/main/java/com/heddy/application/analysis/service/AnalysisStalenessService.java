package com.heddy.application.analysis.service;

import com.heddy.domain.analysis.model.AnalysisJobStatus;
import com.heddy.domain.analysis.port.out.AnalysisJobRepositoryPort;
import com.heddy.domain.analysis.port.out.AnalysisStalenessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * 사진이 바뀐 기록의 최신 분석을 무효화한다. 시술기록의 사진 추가·교체·삭제가 이 지점을 부르고
 * (PR #58), 그동안 구현체가 없어 no-op 폴백으로 아무 일도 하지 않고 있었다.
 *
 * <p>최신 한 건만 대상이다. 과거 분석은 그때 사진의 결과로 이미 확정된 이력이고, 사진이 바뀌었다는
 * 사실이 과거 결과를 틀리게 만들지는 않는다.
 *
 * <p>멱등하다. 이미 무효화됐거나 실패로 끝난 작업은 그냥 둔다 — 사진을 연달아 바꿔도, 같은 변경이
 * 두 번 전달돼도 결과가 같아야 한다. 실패한 작업은 무효화할 결과 자체가 없다.
 *
 * <p>아직 돌고 있는 작업(PENDING·PROCESSING)도 무효화 대상이다. 지금 도착할 결과는 이미 옛
 * 사진의 것이고, 진행 중 자리를 비워 줘야 새 사진으로 다시 분석을 걸 수 있다.
 */
@Service
@RequiredArgsConstructor
public class AnalysisStalenessService implements AnalysisStalenessPort {

    private final AnalysisJobRepositoryPort jobRepositoryPort;

    @Override
    @Transactional
    public void markLatestStale(UUID treatmentRecordId) {
        Instant now = Instant.now();
        jobRepositoryPort.findLatestByRecordId(treatmentRecordId)
                .filter(job -> job.status().canMoveTo(AnalysisJobStatus.STALE))
                .map(job -> job.markStale(now))
                .ifPresent(jobRepositoryPort::update);
    }
}
