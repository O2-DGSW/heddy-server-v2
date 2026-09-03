package com.heddy.domain.style.port.out;

import com.heddy.domain.style.model.SavedStyle;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedStyleRepositoryPort {

    SavedStyle insert(SavedStyle savedStyle);

    /** 내 후보를 최신 저장순으로 읽는다. */
    List<SavedStyle> findAllByUserId(UUID userId);

    /**
     * 소유자 조건을 질의에 실어 한 건을 읽는다. 남의 후보는 없는 후보와 같은 결과여야
     * 존재 여부가 새지 않는다(시술기록 #31 컨벤션).
     */
    Optional<SavedStyle> findByIdAndUserId(UUID savedStyleId, UUID userId);

    /** 후보 한 건을 삭제한다. 지운 행이 없으면 false. */
    boolean deleteById(UUID savedStyleId);

    /** 소유자 조건을 포함해 여러 후보를 최신 저장순으로 읽는다. 없는 식별자는 결과에서 빠진다. */
    List<SavedStyle> findAllByUserIdAndIds(UUID userId, Collection<UUID> savedStyleIds);

    /** 새 카탈로그에 연결된 내 저장 스타일 식별자만 추천 입력으로 읽는다. */
    List<UUID> findHairstyleIdsByUserId(UUID userId);
}
