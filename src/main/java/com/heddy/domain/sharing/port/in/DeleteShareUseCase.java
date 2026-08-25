package com.heddy.domain.sharing.port.in;

import java.util.UUID;

/**
 * 공유 철회. 행을 지우지 않고 즉시 REVOKED 로 전이해 공개 조회를 차단한다(스펙 11.5).
 * 이미 철회된 공유에 다시 호출해도 204 다 — DELETE 는 멱등이어야 한다.
 */
public interface DeleteShareUseCase {

    void delete(Command command);

    record Command(UUID requesterId, UUID shareId) {
    }
}
