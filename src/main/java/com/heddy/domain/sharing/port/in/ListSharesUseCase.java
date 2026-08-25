package com.heddy.domain.sharing.port.in;

import com.heddy.domain.sharing.model.Share;
import com.heddy.domain.sharing.model.ShareStatus;

import java.util.List;
import java.util.UUID;

public interface ListSharesUseCase {

    /** 내 공유를 최신순으로 페이지 조회한다. 결과가 없으면 빈 목록이지 오류가 아니다. */
    Result list(Query query);

    /** 상태 필터는 생략 가능하다. 전체 조회면 null 이다. */
    record Query(UUID requesterId, ShareStatus status, int page, int size) {
    }

    record Result(List<Share> items, int page, int size, long totalElements) {
    }
}
