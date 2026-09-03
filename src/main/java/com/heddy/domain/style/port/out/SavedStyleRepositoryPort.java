package com.heddy.domain.style.port.out;

import com.heddy.domain.style.model.SavedStyle;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedStyleRepositoryPort {

    SavedStyle insert(SavedStyle savedStyle);

    /** 메모가 수정된 후보를 저장한다. */
    SavedStyle update(SavedStyle savedStyle);

    /** 내 후보를 최신 저장순으로 읽는다. */
    List<SavedStyle> findAllByUserId(UUID userId);

    /** 소유자 조건을 포함해 한 건을 읽어 다른 사용자의 후보 존재 여부를 감춘다. */
    Optional<SavedStyle> findByIdAndUserId(UUID savedStyleId, UUID userId);

    /** 후보 한 건을 삭제한다. 지운 행이 없으면 false. */
    boolean deleteById(UUID savedStyleId);

    /** 소유자 조건을 포함해 여러 후보를 최신 저장순으로 읽는다. */
    List<SavedStyle> findAllByUserIdAndIds(UUID userId, Collection<UUID> savedStyleIds);

    /** 카탈로그에 연결된 내 저장 스타일 식별자만 추천 입력으로 읽는다. */
    List<UUID> findHairstyleIdsByUserId(UUID userId);

    /** 회원 탈퇴 시 사용자의 저장 후보를 모두 제거한다. */
    void deleteAllByUserId(UUID userId);
}
