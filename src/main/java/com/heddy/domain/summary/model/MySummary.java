package com.heddy.domain.summary.model;

/**
 * 홈 화면 상단 요약 타일 4종의 카운트.
 *
 * <p>네 값의 단위는 모두 "개수"지만 세는 대상이 다르다. {@code sharedRecordCount} 는 공유
 * 링크가 아니라 <b>시술기록</b> 을 센다 — 링크 하나가 기록 여러 개를 담을 수도, 기록 없이
 * 후보 스타일만 담을 수도 있고 같은 기록으로 링크를 여러 번 만들 수도 있어, 링크 수를 세면
 * 시술기록 수와 비교 자체가 성립하지 않는다.
 */
public record MySummary(
        long treatmentRecordCount,
        long analyzedRecordCount,
        long savedStyleCount,
        long sharedRecordCount
) {
}
