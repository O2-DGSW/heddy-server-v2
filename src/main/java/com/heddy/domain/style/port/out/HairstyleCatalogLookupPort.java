package com.heddy.domain.style.port.out;

import com.heddy.domain.style.model.CatalogHairstyle;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 후보 저장이 필요한 만큼만 카탈로그를 들여다본다.
 *
 * <p>추천 쪽 조회 포트를 쓰지 않는 이유는 그쪽이 "추천 프로필이 완비된" 스타일만 돌려주기
 * 때문이다. AR 로 시연할 수 있는 스타일이라고 해서 추천 메타데이터까지 갖췄으리라는 보장은
 * 없고, 저장이 그 조건에 묶이면 화면에서 고른 스타일이 이유 없이 저장되지 않는다.
 */
public interface HairstyleCatalogLookupPort {

    /** 활성 스타일 한 건. 내려간 스타일은 새로 저장할 수 없으므로 결과에서 뺀다. */
    Optional<CatalogHairstyle> findActiveById(UUID hairstyleId);

    /** 카드 이미지의 마지막 대안인 썸네일 파일들을 한 번에 읽는다. */
    Map<UUID, UUID> findThumbnailFileIds(Collection<UUID> hairstyleIds);
}
