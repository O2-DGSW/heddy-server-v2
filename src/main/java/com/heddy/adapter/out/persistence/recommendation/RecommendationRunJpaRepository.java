package com.heddy.adapter.out.persistence.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

interface RecommendationRunJpaRepository extends JpaRepository<RecommendationRunEntity, UUID> {
    Optional<RecommendationRunEntity> findFirstByUserIdAndStrategyAndInputHashAndStatusOrderByGeneratedAtDescRecommendationRunIdDesc(
            UUID userId, String strategy, String inputHash, String status);
    Optional<RecommendationRunEntity> findFirstByUserIdOrderByGeneratedAtDescRecommendationRunIdDesc(UUID userId);
    Optional<RecommendationRunEntity> findByRecommendationRunIdAndUserId(UUID recommendationRunId, UUID userId);

    interface HairstyleExposure {
        UUID getHairstyleId();
        Long getExposureCount();
    }

    @Query(value = """
            SELECT item.hairstyle_id AS "hairstyleId", count(*) AS "exposureCount"
            FROM recommendation_items item
            JOIN (
                SELECT recommendation_run_id
                FROM recommendation_runs
                WHERE user_id = :userId AND input_hash = :inputHash
                ORDER BY generated_at DESC, recommendation_run_id DESC
                LIMIT 20
            ) recent ON recent.recommendation_run_id = item.recommendation_run_id
            GROUP BY item.hairstyle_id
            """, nativeQuery = true)
    List<HairstyleExposure> findRecentExposureCounts(
            @Param("userId") UUID userId, @Param("inputHash") String inputHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE recommendation_runs SET status = 'STALE'
            WHERE status = 'ACTIVE' AND recommendation_run_id IN (
                SELECT DISTINCT item.recommendation_run_id
                FROM recommendation_items item
                JOIN recommendation_reference_records reference
                  ON reference.recommendation_item_id = item.recommendation_item_id
                WHERE reference.record_id = :recordId
            )
            """, nativeQuery = true)
    int markStaleByReferenceRecordId(@Param("recordId") UUID recordId);
}
