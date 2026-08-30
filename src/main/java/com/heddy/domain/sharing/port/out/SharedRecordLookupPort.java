package com.heddy.domain.sharing.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * 주어진 시술기록들 중 지금 공유 중인 것만 골라낸다. 목록의 "공유중" 배지가 유일한 소비자다.
 *
 * <p>기록 하나씩 묻지 않고 집합으로 받는 이유는 목록이 페이지 단위이기 때문이다 — 기록마다
 * 질의하면 페이지 크기만큼 왕복이 늘어난다.
 *
 * <p>판정 기준은 공개 조회와 같아야 한다({@code Share#isViewable}). 만료는 상태로 저장하지
 * 않으므로 {@code status} 만 보면 만료된 링크가 공유중으로 남는다. 그래서 기준 시각을 인자로
 * 받는다.
 */
@FunctionalInterface
public interface SharedRecordLookupPort {

    /**
     * @param ownerId    기록 소유자. 남의 공유가 내 배지를 켜지 못하게 조건에 함께 싣는다
     * @param recordIds  판정할 기록들. 비어 있으면 질의 없이 빈 집합이다
     * @param now        만료 판정 기준 시각
     * @return 공유 중인 기록 ID. 입력에 없던 ID 는 담기지 않는다
     */
    Set<UUID> findSharedRecordIds(UUID ownerId, Collection<UUID> recordIds, Instant now);
}
