package com.heddy.adapter.out.persistence.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface RecommendationItemJpaRepository extends JpaRepository<RecommendationItemEntity, UUID> {
    List<RecommendationItemEntity> findByRecommendationRunIdOrderByDisplayRankAsc(UUID runId);
}
