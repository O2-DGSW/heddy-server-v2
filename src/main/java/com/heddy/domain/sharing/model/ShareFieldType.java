package com.heddy.domain.sharing.model;

/**
 * 공유 링크에 노출할 항목 6종(스펙 11.1). 공개 조회는 여기에 선택된 것만 직렬화 단계부터
 * 내보내므로, 값 자체가 접근 제어의 경계다.
 */
public enum ShareFieldType {
    PHOTOS,
    TREATMENT_DETAILS,
    SATISFACTION,
    CAUTIONS,
    MEMO,
    SAVED_STYLES
}
