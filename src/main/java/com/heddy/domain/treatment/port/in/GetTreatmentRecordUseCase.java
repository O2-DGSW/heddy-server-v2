package com.heddy.domain.treatment.port.in;

import com.heddy.domain.treatment.model.TreatmentRecord;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * 시술기록 단건 조회 유스케이스. 남의 기록은 존재 여부가 노출되지 않도록 없는 기록과
 * 같은 RESOURCE_NOT_FOUND 로 번역한다(#31).
 */
public interface GetTreatmentRecordUseCase {

    Result get(Query query);

    record Query(UUID requesterId, UUID recordId) {
    }

    /** {@code photoUrls} 키는 photo_id 다. URL 을 저장하지 않고 조회 때마다 새로 발급한다. */
    record Result(TreatmentRecord record, Map<UUID, URI> photoUrls) {
    }
}
