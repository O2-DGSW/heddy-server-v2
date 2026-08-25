package com.heddy.domain.sharing.port.in;

import com.heddy.domain.sharing.model.Share;
import com.heddy.domain.sharing.model.ShareFieldType;

import java.util.Set;
import java.util.UUID;

public interface CreateShareUseCase {

    /** 공유를 만들고 링크 URL 을 딱 한 번 돌려준다. 토큰 원문은 어디에도 저장되지 않는다. */
    Result create(Command command);

    /**
     * @param recordIds      공유할 시술기록. 비어 있어도 되지만 후보 스타일과 함께 하나는 있어야 한다
     * @param savedStyleIds  공유할 후보 스타일. {@code recordIds} 와 합쳐 1개 이상이어야 한다
     * @param fields         노출 항목. 1개 이상이어야 한다
     * @param expiresInDays  유효기간(일). 비우면 도메인 기본값 7일
     */
    record Command(
            UUID userId,
            Set<UUID> recordIds,
            Set<UUID> savedStyleIds,
            Set<ShareFieldType> fields,
            Integer expiresInDays
    ) {
    }

    /**
     * @param shareUrl 서버가 만든 원문 토큰을 담은 링크. 생성 응답으로만 내보내고 다시 조회할 수 없다
     */
    record Result(Share share, String shareUrl) {
    }
}
