package com.heddy.domain.sharing.model;

import java.util.List;

/** 페이지로 잘린 공유 목록과 전체 건수. 스프링 Page 타입이 도메인으로 새지 않게 하기 위한 그릇이다. */
public record SharePage(List<Share> items, long totalElements) {
}
