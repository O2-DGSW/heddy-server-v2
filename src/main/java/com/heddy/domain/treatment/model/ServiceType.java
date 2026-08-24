package com.heddy.domain.treatment.model;

/** 시술 종류. API 명세 v2 의 service_types 값 목록이며 하나의 기록에 여러 개가 동시에 붙는다. */
public enum ServiceType {
    CUT,
    PERM,
    COLOR,
    BLEACH,
    CLINIC,
    STYLING,
    OTHER
}
