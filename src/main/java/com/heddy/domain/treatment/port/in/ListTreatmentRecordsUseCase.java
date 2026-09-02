package com.heddy.domain.treatment.port.in;

import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.analysis.model.AnalysisJobStatus;
import com.heddy.domain.treatment.model.TreatmentRecord;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 내 시술기록을 필터와 페이지 조건으로 조회한다. */
public interface ListTreatmentRecordsUseCase {

    Result list(Query query);

    record Query(
            UUID requesterId,
            ServiceType serviceType,
            String designerName,
            String salonName,
            Instant from,
            Instant to,
            int page,
            int size,
            String sort
    ) {
    }

    /**
     * @param analysisStatus 최신 분석 상태. 한 번도 분석하지 않은 기록은 null 이다.
     *                       이름을 문자열로 옮기지 않는 이유는 허용값의 출처를 하나로 두기
     *                       위해서다 — 옮기는 순간 열거형과 문자열 목록이 따로 늙는다
     * @param shared 지금 공유 중인 기록인지. 목록의 "공유중" 배지 하나가 쓴다
     */
    record Item(TreatmentRecord record, URI thumbnailUrl, AnalysisJobStatus analysisStatus,
                boolean shared) {
    }

    record Result(List<Item> items, int page, int size, long totalElements) {
        public Result {
            items = List.copyOf(items);
        }
    }
}
