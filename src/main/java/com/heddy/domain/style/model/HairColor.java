package com.heddy.domain.style.model;

import java.util.UUID;

/**
 * 헤어 컬러 카탈로그의 한 항목. {@code hairstyle_assets} 와 짝을 이루는 공용 카탈로그로,
 * 추천 결과·저장한 후보·AR 팔레트가 모두 이 표를 가리킨다.
 *
 * <p>{@code code} 를 따로 두는 이유는 표시명이 마케팅 사정으로 바뀌어도 클라이언트 분기와
 * AR 매핑이 흔들리지 않게 하기 위해서다. 식별자는 환경 간 고정이라 시드로 심는다.
 */
public record HairColor(
        UUID colorId,
        String code,
        String name,
        String hexCode,
        int sortOrder
) {
}
