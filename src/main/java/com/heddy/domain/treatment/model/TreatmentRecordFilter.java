package com.heddy.domain.treatment.model;

import java.time.Instant;
import java.util.UUID;

/** 시술기록 목록을 소유자와 선택 필터로 조회하기 위한 도메인 검색 조건. */
public record TreatmentRecordFilter(
        UUID userId,
        ServiceType serviceType,
        String designerName,
        String salonName,
        Instant from,
        Instant to,
        int page,
        int size,
        boolean ascending
) {
}
