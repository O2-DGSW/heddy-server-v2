package com.heddy.domain.style.port.out;

import com.heddy.domain.style.model.SavedStyle;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SavedStyleRepositoryPort {

    SavedStyle insert(SavedStyle savedStyle);

    /** 소유자 조건을 포함해 여러 후보를 최신 저장순으로 읽는다. 없는 식별자는 결과에서 빠진다. */
    List<SavedStyle> findAllByUserIdAndIds(UUID userId, Collection<UUID> savedStyleIds);

    /** 새 카탈로그에 연결된 내 저장 스타일 식별자만 추천 입력으로 읽는다. */
    List<UUID> findHairstyleIdsByUserId(UUID userId);
}
