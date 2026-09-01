package com.heddy.adapter.out.persistence.recommendation;

import com.heddy.domain.recommendation.model.RecommendationRun;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "recommendation_runs")
@EntityListeners(AuditingEntityListener.class)
class RecommendationRunEntity {
    @Id @Column(name = "recommendation_run_id", nullable = false, updatable = false)
    private UUID recommendationRunId;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(nullable = false, length = 30, updatable = false)
    private String strategy;
    @Column(nullable = false, length = 20)
    private String status;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_snapshot_json", nullable = false, columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> inputSnapshot = new LinkedHashMap<>();
    @Column(name = "input_hash", nullable = false, length = 64, updatable = false)
    private String inputHash;
    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RecommendationRunEntity() { }

    RecommendationRunEntity(RecommendationRun run, String canonicalSnapshot) {
        recommendationRunId = run.recommendationRunId();
        userId = run.userId();
        strategy = run.strategy().name();
        status = run.status().name();
        inputSnapshot = Map.of("fallback", run.fallback(), "canonical", canonicalSnapshot);
        inputHash = run.inputHash();
        generatedAt = run.generatedAt();
    }

    RecommendationRun toDomain(List<com.heddy.domain.recommendation.model.RecommendationItem> items) {
        return new RecommendationRun(recommendationRunId, userId,
                RecommendationRun.Strategy.valueOf(strategy), RecommendationRun.Status.valueOf(status),
                inputHash, Boolean.TRUE.equals(inputSnapshot.get("fallback")), generatedAt, items);
    }

    UUID recommendationRunId() { return recommendationRunId; }
}
