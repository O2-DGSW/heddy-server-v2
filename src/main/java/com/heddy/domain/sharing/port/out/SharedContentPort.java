package com.heddy.domain.sharing.port.out;

import com.heddy.domain.sharing.model.SharedContentSnapshot;

import java.util.Set;
import java.util.UUID;

/**
 * 공유 대상 시술기록의 읽기 전용 조회. 후보 스타일 도메인이 자리 잡기 전까지 기록 쪽만
 * 제공한다. 소유자 화면용 데이터가 아니라 공개 링크에 내보낼 최소한만 담는다.
 */
public interface SharedContentPort {

    /**
     * @param recordIds 공유가 가리키는 기록들. 없거나 지워진 기록은 조용히 결과에서 뺀다 —
     *                  링크 전체를 깨뜨리는 것보다 남은 기록만 보여주는 편이 낫다
     */
    SharedContentSnapshot load(UUID ownerId, Set<UUID> recordIds);
}
