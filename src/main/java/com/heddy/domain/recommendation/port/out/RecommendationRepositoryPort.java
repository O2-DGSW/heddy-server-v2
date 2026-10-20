package com.heddy.domain.recommendation.port.out;

import com.heddy.domain.recommendation.model.RecommendationRun;

import java.util.Optional;
import java.util.Map;
import java.util.UUID;

public interface RecommendationRepositoryPort {
    RecommendationRun insert(RecommendationRun run, String canonicalInputSnapshot);

    Optional<RecommendationRun> findActiveByInputHash(UUID userId, String strategy, String inputHash);

    Optional<RecommendationRun> findLatestByUserId(UUID userId);

    /** 동일 사용자·입력의 최근 20회 추천에서 스타일별 노출 횟수. */
    Map<UUID, Long> findRecentExposureCounts(UUID userId, String inputHash);

    Optional<RecommendationRun> findByIdAndUserId(UUID recommendationRunId, UUID userId);
}
