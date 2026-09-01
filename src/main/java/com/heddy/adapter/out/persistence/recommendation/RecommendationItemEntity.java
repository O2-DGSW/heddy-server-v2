package com.heddy.adapter.out.persistence.recommendation;

import com.heddy.domain.recommendation.model.HairstyleCandidate.ManagementDifficulty;
import com.heddy.domain.recommendation.model.RecommendationItem;
import com.heddy.domain.recommendation.model.RecommendationReason;
import com.heddy.domain.recommendation.model.RecommendationReference;
import com.heddy.domain.recommendation.model.ScoreBreakdown;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "recommendation_items")
class RecommendationItemEntity {
    @Id @Column(name = "recommendation_item_id", nullable = false, updatable = false)
    private UUID recommendationItemId;
    @Column(name = "recommendation_run_id", nullable = false, updatable = false)
    private UUID recommendationRunId;
    @Column(name = "hairstyle_id", nullable = false, updatable = false)
    private UUID hairstyleId;
    @Column(name = "color_id", updatable = false)
    private UUID colorId;
    @Column(name = "display_rank", nullable = false, updatable = false)
    private int displayRank;
    @Column(nullable = false, precision = 5, scale = 2, updatable = false)
    private BigDecimal score;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_breakdown_json", nullable = false, columnDefinition = "jsonb", updatable = false)
    private Map<String, Double> scoreBreakdown = new LinkedHashMap<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reasons_json", nullable = false, columnDefinition = "jsonb", updatable = false)
    private List<Map<String, Object>> reasons = List.of();
    @Column(name = "management_difficulty", nullable = false, length = 20, updatable = false)
    private String managementDifficulty;
    @Column(name = "estimated_daily_care_minutes", nullable = false, updatable = false)
    private int estimatedDailyCareMinutes;

    protected RecommendationItemEntity() { }

    RecommendationItemEntity(UUID runId, RecommendationItem item) {
        recommendationItemId = item.recommendationItemId();
        recommendationRunId = runId;
        hairstyleId = item.hairstyleId();
        colorId = item.colorId();
        displayRank = item.displayRank();
        score = item.score();
        scoreBreakdown = breakdownMap(item.scoreBreakdown());
        reasons = item.reasons().stream().map(reason -> Map.<String, Object>of(
                "code", reason.code().name(), "params", reason.params())).toList();
        managementDifficulty = item.managementDifficulty().name();
        estimatedDailyCareMinutes = item.estimatedDailyCareMinutes();
    }

    RecommendationItem toDomain(RecommendationReference reference) {
        return new RecommendationItem(recommendationItemId, hairstyleId, colorId, displayRank,
                score, toBreakdown(), reasons.stream().map(RecommendationItemEntity::toReason).toList(),
                ManagementDifficulty.valueOf(managementDifficulty), estimatedDailyCareMinutes, reference);
    }

    UUID recommendationItemId() { return recommendationItemId; }

    private static Map<String, Double> breakdownMap(ScoreBreakdown value) {
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("history", value.historyScore());
        result.put("saved_style", value.savedStyleScore());
        result.put("preferred_tag", value.preferredTagScore());
        result.put("hair_compatibility", value.hairCompatibilityScore());
        result.put("care_time", value.careTimeScore());
        result.put("raw", value.rawScore());
        result.put("available_maximum", value.availableMaximumScore());
        result.put("final", value.finalScore());
        return result;
    }

    private ScoreBreakdown toBreakdown() {
        return new ScoreBreakdown(value("history"), value("saved_style"), value("preferred_tag"),
                value("hair_compatibility"), value("care_time"), value("raw"),
                value("available_maximum"), value("final"));
    }

    private double value(String key) {
        return scoreBreakdown.getOrDefault(key, 0.0);
    }

    @SuppressWarnings("unchecked")
    private static RecommendationReason toReason(Map<String, Object> value) {
        Map<String, Object> rawParams = (Map<String, Object>) value.getOrDefault("params", Map.of());
        Map<String, String> params = new LinkedHashMap<>();
        rawParams.forEach((key, raw) -> params.put(key, String.valueOf(raw)));
        return new RecommendationReason(
                RecommendationReason.Code.valueOf(String.valueOf(value.get("code"))), params);
    }
}
