package com.heddy.adapter.in.web.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.recommendation.model.RecommendationReason;
import com.heddy.domain.recommendation.model.RecommendationBasis;
import com.heddy.domain.recommendation.model.RecommendationReference;
import com.heddy.domain.recommendation.port.in.RecommendationResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RecommendationResponse(
        @JsonProperty("recommendation_run_id") UUID recommendationRunId,
        String strategy,
        String status,
        @JsonProperty("generated_at") Instant generatedAt,
        boolean fallback,
        @JsonProperty("recommendation_basis") Basis recommendationBasis,
        List<Item> items
) {
    public static RecommendationResponse from(RecommendationResult result) {
        return new RecommendationResponse(result.recommendationRunId(), result.strategy().name(),
                result.status().name(), result.generatedAt(), result.fallback(),
                Basis.from(result.recommendationBasis()),
                result.items().stream().map(Item::from).toList());
    }

    public record Basis(
            @JsonProperty("treatment_history") TreatmentHistory treatmentHistory,
            @JsonProperty("ar_candidate_style_count") int arCandidateStyleCount,
            @JsonProperty("style_preferences") StylePreferences stylePreferences,
            @JsonProperty("current_hair") CurrentHair currentHair,
            @JsonProperty("available_care_time_minutes") Integer availableCareTimeMinutes
    ) {
        static Basis from(RecommendationBasis basis) {
            if (basis == null) {
                return null;
            }
            return new Basis(
                    new TreatmentHistory(basis.treatmentHistory().count(),
                            basis.treatmentHistory().highestSatisfaction()),
                    basis.arCandidateStyleCount(),
                    new StylePreferences(basis.stylePreferences().preferredCount(),
                            basis.stylePreferences().excludedCount()),
                    CurrentHair.from(basis.currentHair()), basis.availableCareTimeMinutes());
        }
    }

    public record TreatmentHistory(
            long count,
            @JsonProperty("highest_satisfaction") Integer highestSatisfaction
    ) { }

    public record StylePreferences(
            @JsonProperty("preferred_count") int preferredCount,
            @JsonProperty("excluded_count") int excludedCount
    ) { }

    public record CurrentHair(
            @JsonProperty("hair_type") String hairType,
            @JsonProperty("hair_condition") String hairCondition,
            @JsonProperty("hair_length") String hairLength,
            @JsonProperty("hair_thickness") String hairThickness
    ) {
        static CurrentHair from(RecommendationBasis.CurrentHair hair) {
            return hair == null ? null : new CurrentHair(name(hair.hairType()),
                    name(hair.hairCondition()), name(hair.hairLength()),
                    name(hair.hairThickness()));
        }

        private static String name(Enum<?> value) {
            return value == null ? null : value.name();
        }
    }

    public record Item(
            int rank,
            BigDecimal score,
            Hairstyle hairstyle,
            @JsonProperty("recommended_color") UUID recommendedColor,
            @JsonProperty("management_difficulty") String managementDifficulty,
            @JsonProperty("estimated_daily_care_minutes") int estimatedDailyCareMinutes,
            List<Reason> reasons,
            @JsonProperty("reference_records") List<ReferenceRecord> referenceRecords
    ) {
        static Item from(RecommendationResult.Item value) {
            var item = value.recommendation();
            RecommendationReference reference = item.referenceRecord();
            return new Item(item.displayRank(), item.score(), new Hairstyle(
                    value.hairstyle().hairstyleId(), value.hairstyle().styleName(),
                    value.thumbnailUrl() == null ? null : value.thumbnailUrl().toString(),
                    value.hairstyle().arMode(), value.hairstyle().assetVersion()), item.colorId(),
                    item.managementDifficulty().name(), item.estimatedDailyCareMinutes(),
                    item.reasons().stream().map(Reason::from).toList(),
                    reference == null ? List.of() : List.of(ReferenceRecord.from(reference)));
        }
    }

    public record Hairstyle(
            @JsonProperty("hairstyle_id") UUID hairstyleId,
            @JsonProperty("style_name") String styleName,
            @JsonProperty("thumbnail_url") String thumbnailUrl,
            @JsonProperty("ar_mode") String arMode,
            @JsonProperty("asset_version") String assetVersion
    ) { }

    public record Reason(String code, String message, Map<String, String> params) {
        static Reason from(RecommendationReason reason) {
            return new Reason(reason.code().name(), message(reason), reason.params());
        }

        private static String message(RecommendationReason reason) {
            return switch (reason.code()) {
                case SIMILAR_HIGH_SATISFACTION_HISTORY -> "이전에 만족한 시술과 유사합니다.";
                case SAVED_STYLE_MATCH -> "이전에 저장한 스타일입니다.";
                case PREFERRED_TAG_MATCH -> "선호하는 ‘"
                        + reason.params().getOrDefault("tag_name", "스타일") + "’ 태그와 일치합니다.";
                case HAIR_PROFILE_COMPATIBLE -> "현재 모발 특성과 잘 맞습니다.";
                case CARE_TIME_FIT -> "평소 관리 가능 시간에 적합합니다.";
                case EDITORIAL_FALLBACK -> "기본 추천 우선순위를 반영했습니다.";
            };
        }
    }

    public record ReferenceRecord(
            @JsonProperty("record_id") UUID recordId,
            @JsonProperty("performed_at") Instant performedAt,
            Integer satisfaction
    ) {
        static ReferenceRecord from(RecommendationReference reference) {
            return new ReferenceRecord(reference.recordId(), reference.performedAt(),
                    reference.satisfaction());
        }
    }
}
