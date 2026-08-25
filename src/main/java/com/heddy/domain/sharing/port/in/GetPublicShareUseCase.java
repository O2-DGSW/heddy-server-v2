package com.heddy.domain.sharing.port.in;

import com.heddy.domain.sharing.model.SharedContentView;

import java.time.Instant;

/**
 * 무인증 공개 조회 유스케이스(스펙 11.6). 토큰 원문을 해시로 대조하고 철회·만료를 매 요청
 * 검증한다. 토큰이 틀렸을 때는 SHARE_TOKEN_INVALID 로 존재 여부마저 부정한다.
 */
public interface GetPublicShareUseCase {

    Result get(Query query);

    /** 요청 경로의 토큰 원문. 서버 어디에도 저장되지 않는 값이다. */
    record Query(String shareToken) {
    }

    /**
     * @param includesSavedStyles SAVED_STYLES 항목이 선택됐는지. 후보 스타일 도메인이 없어
     *                            지금은 빈 배열 직렬화 여부만 결정한다
     */
    record Result(
            Instant expiresAt,
            boolean includesSavedStyles,
            SharedContentView content
    ) {
    }
}
