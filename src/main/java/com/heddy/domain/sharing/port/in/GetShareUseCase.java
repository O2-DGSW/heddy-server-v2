package com.heddy.domain.sharing.port.in;

import com.heddy.domain.sharing.model.Share;

import java.util.UUID;

/** 내 공유의 설정 상세를 조회한다. 남의 공유는 없는 공유와 같은 RESOURCE_NOT_FOUND 로 은닉한다. */
public interface GetShareUseCase {

    Share get(Query query);

    record Query(UUID requesterId, UUID shareId) {
    }
}
