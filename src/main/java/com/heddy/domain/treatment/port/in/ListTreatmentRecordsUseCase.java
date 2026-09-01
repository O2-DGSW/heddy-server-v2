package com.heddy.domain.treatment.port.in;

import com.heddy.domain.treatment.model.ServiceType;
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
     * @param shared 지금 공유 중인 기록인지. 목록의 "공유중" 배지 하나가 쓴다
     */
    record Item(TreatmentRecord record, URI thumbnailUrl, String analysisStatus, boolean shared) {
    }

    record Result(List<Item> items, int page, int size, long totalElements) {
        public Result {
            items = List.copyOf(items);
        }
    }
}
