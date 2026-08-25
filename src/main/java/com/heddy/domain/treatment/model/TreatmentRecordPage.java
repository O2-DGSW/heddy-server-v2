package com.heddy.domain.treatment.model;

import java.util.List;

/** 저장 기술의 페이지 타입을 도메인 밖으로 노출하지 않는 시술기록 조회 결과. */
public record TreatmentRecordPage(
        List<TreatmentRecord> items,
        long totalElements
) {
    public TreatmentRecordPage {
        items = List.copyOf(items);
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements 는 음수일 수 없습니다.");
        }
    }
}
