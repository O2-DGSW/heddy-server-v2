package com.heddy.domain.treatment.model;

/** 사용자의 전체 시술 이력 건수와 입력된 만족도 중 최고값. */
public record TreatmentHistorySummary(long count, Integer highestSatisfaction) { }
