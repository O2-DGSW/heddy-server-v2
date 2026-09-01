package com.heddy.domain.recommendation.port.out;

import com.heddy.domain.recommendation.model.HairstyleCandidate;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface HairstyleCatalogRepositoryPort {
    /** READY 썸네일과 완전한 추천 프로필을 가진 활성 후보를 배치 조회한다. */
    List<HairstyleCandidate> findEligibleCandidates();

    List<HairstyleCandidate> findAllByIds(Collection<UUID> hairstyleIds);
}
