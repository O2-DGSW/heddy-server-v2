package com.heddy.domain.sharing.port.out;

import com.heddy.domain.sharing.model.Share;
import com.heddy.domain.sharing.model.SharePage;
import com.heddy.domain.sharing.model.ShareStatus;

import java.util.Optional;
import java.util.UUID;

public interface ShareRepositoryPort {

    /** 공유와 선택된 대상·항목 조인 행들을 한 번에 저장한다. */
    Share insert(Share share);

    /** 수정된 내용을 저장한다. 대상·항목 조인 행 중 항목만 교체한다. */
    Share update(Share share);

    /**
     * 소유자 조건까지 걸어 조회한다. 남의 공유는 없는 공유와 같은 404 이고, 질의 횟수도 같아야
     * 존재 여부가 새지 않는다(시술기록 #31 컨벤션).
     */
    Optional<Share> findByIdAndUserId(UUID shareId, UUID userId);

    /** 토큰 해시 대조로 조회한다. 공개 조회(#51)의 유일한 진입 경로다. */
    Optional<Share> findByTokenHash(String tokenHash);

    /** 소유자의 공유를 최신순으로 페이지 조회한다. 상태 필터는 생략 가능하다. */
    SharePage findPage(UUID userId, ShareStatus status, int page, int size);
}
