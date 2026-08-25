package com.heddy.domain.sharing.port.in;

import com.heddy.domain.sharing.model.Share;
import com.heddy.domain.sharing.model.ShareFieldType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** 노출 항목과 만료 시각만 수정한다. 대상(기록·후보)은 스펙 11.4 의 수정 범위가 아니다. */
public interface UpdateShareUseCase {

    Share update(Command command);

    record Command(
            UUID requesterId,
            UUID shareId,
            Patch<Set<ShareFieldType>> fields,
            Patch<Instant> expiresAt
    ) {
    }

    /** {@code present=false}는 미전달, {@code present=true,value=null}은 명시적 삭제다. */
    record Patch<T>(boolean present, T value) {
        public static <T> Patch<T> absent() {
            return new Patch<>(false, null);
        }

        public static <T> Patch<T> present(T value) {
            return new Patch<>(true, value);
        }

        public T orElse(T current) {
            return present ? value : current;
        }
    }
}
