package com.heddy.adapter.out.persistence.recommendation;

import com.heddy.domain.recommendation.model.RecommendationRun;
import com.heddy.domain.recommendation.model.RecommendationBasis;
import com.heddy.domain.account.model.HairProfile.HairCondition;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.account.model.HairProfile.HairThickness;
import com.heddy.domain.account.model.HairProfile.HairType;
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
        inputSnapshot = inputSnapshot(run, canonicalSnapshot);
        inputHash = run.inputHash();
        generatedAt = run.generatedAt();
    }

    RecommendationRun toDomain(List<com.heddy.domain.recommendation.model.RecommendationItem> items) {
        return new RecommendationRun(recommendationRunId, userId,
                RecommendationRun.Strategy.valueOf(strategy), RecommendationRun.Status.valueOf(status),
                inputHash, Boolean.TRUE.equals(inputSnapshot.get("fallback")),
                recommendationBasis(), generatedAt, items);
    }

    UUID recommendationRunId() { return recommendationRunId; }

    private static Map<String, Object> inputSnapshot(
            RecommendationRun run,
            String canonicalSnapshot
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("fallback", run.fallback());
        snapshot.put("canonical", canonicalSnapshot);
        if (run.recommendationBasis() != null) {
            snapshot.put("recommendation_basis", basisMap(run.recommendationBasis()));
        }
        return snapshot;
    }

    private static Map<String, Object> basisMap(RecommendationBasis basis) {
        Map<String, Object> treatmentHistory = new LinkedHashMap<>();
        treatmentHistory.put("count", basis.treatmentHistory().count());
        treatmentHistory.put("highest_satisfaction", basis.treatmentHistory().highestSatisfaction());

        Map<String, Object> stylePreferences = new LinkedHashMap<>();
        stylePreferences.put("preferred_count", basis.stylePreferences().preferredCount());
        stylePreferences.put("excluded_count", basis.stylePreferences().excludedCount());

        Map<String, Object> value = new LinkedHashMap<>();
        value.put("treatment_history", treatmentHistory);
        value.put("ar_candidate_style_count", basis.arCandidateStyleCount());
        value.put("style_preferences", stylePreferences);
        if (basis.currentHair() != null) {
            Map<String, Object> currentHair = new LinkedHashMap<>();
            putEnum(currentHair, "hair_type", basis.currentHair().hairType());
            putEnum(currentHair, "hair_condition", basis.currentHair().hairCondition());
            putEnum(currentHair, "hair_length", basis.currentHair().hairLength());
            putEnum(currentHair, "hair_thickness", basis.currentHair().hairThickness());
            value.put("current_hair", currentHair);
        }
        value.put("available_care_time_minutes", basis.availableCareTimeMinutes());
        return value;
    }

    private RecommendationBasis recommendationBasis() {
        Object raw = inputSnapshot.get("recommendation_basis");
        if (!(raw instanceof Map<?, ?> basis)) {
            return null;
        }
        Map<?, ?> treatmentHistory = map(basis.get("treatment_history"));
        Map<?, ?> stylePreferences = map(basis.get("style_preferences"));
        Map<?, ?> currentHair = map(basis.get("current_hair"));
        RecommendationBasis.CurrentHair hair = currentHair.isEmpty() ? null
                : new RecommendationBasis.CurrentHair(
                        enumValue(currentHair, "hair_type", HairType.class),
                        enumValue(currentHair, "hair_condition", HairCondition.class),
                        enumValue(currentHair, "hair_length", HairLength.class),
                        enumValue(currentHair, "hair_thickness", HairThickness.class));
        return new RecommendationBasis(
                new RecommendationBasis.TreatmentHistory(
                        longValue(treatmentHistory, "count", 0),
                        integer(treatmentHistory, "highest_satisfaction")),
                integer(basis, "ar_candidate_style_count", 0),
                new RecommendationBasis.StylePreferences(
                        integer(stylePreferences, "preferred_count", 0),
                        integer(stylePreferences, "excluded_count", 0)),
                hair,
                integer(basis, "available_care_time_minutes"));
    }

    private static void putEnum(Map<String, Object> target, String key, Enum<?> value) {
        if (value != null) {
            target.put(key, value.name());
        }
    }

    private static Map<?, ?> map(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private static Integer integer(Map<?, ?> source, String key) {
        Object value = source.get(key);
        return value instanceof Number number ? number.intValue() : null;
    }

    private static int integer(Map<?, ?> source, String key, int fallback) {
        Integer value = integer(source, key);
        return value == null ? fallback : value;
    }

    private static long longValue(Map<?, ?> source, String key, long fallback) {
        Object value = source.get(key);
        return value instanceof Number number ? number.longValue() : fallback;
    }

    private static <E extends Enum<E>> E enumValue(
            Map<?, ?> source,
            String key,
            Class<E> type
    ) {
        Object value = source.get(key);
        return value == null ? null : Enum.valueOf(type, String.valueOf(value));
    }
}
