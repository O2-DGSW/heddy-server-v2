package com.heddy.domain.recommendation.service;

import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.recommendation.model.HairstyleCandidate;
import com.heddy.domain.recommendation.model.RecommendationContext;
import com.heddy.domain.recommendation.model.RecommendationReason;
import com.heddy.domain.recommendation.model.ScoreBreakdown;
import com.heddy.domain.recommendation.model.ScoredRecommendation;
import com.heddy.domain.treatment.model.TreatmentRecord;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** 프레임워크와 무관한 RULE_BASED_V1 하드 필터 및 점수 계산기. */
public final class RuleBasedV1Scorer {

    public List<ScoredRecommendation> scoreAll(
            List<HairstyleCandidate> candidates,
            RecommendationContext context
    ) {
        return candidates.stream()
                .map(candidate -> score(candidate, context))
                .flatMap(Optional::stream)
                .sorted(scoreOrder())
                .toList();
    }

    public Optional<ScoredRecommendation> score(
            HairstyleCandidate candidate,
            RecommendationContext context
    ) {
        if (!eligible(candidate, context)) {
            return Optional.empty();
        }

        List<RecommendationReason> reasons = new ArrayList<>();
        HistoryResult history = historyScore(candidate, context, reasons);
        double savedStyleScore = savedStyleScore(candidate, context, reasons);
        double preferredTagScore = preferredTagScore(candidate, context, reasons);
        HairResult hair = hairScore(candidate, context, reasons);
        double careTimeScore = careTimeScore(candidate, context, reasons);

        double availableMaximum = 0;
        if (context.recentTreatments().stream().anyMatch(record -> record.satisfaction() != null)) {
            availableMaximum += 40;
        }
        if (!context.savedHairstyleIds().isEmpty() || !context.preferredTagIds().isEmpty()) {
            availableMaximum += 30;
        }
        availableMaximum += hair.availableMaximum();
        if (context.hairProfile() != null
                && context.hairProfile().availableCareTimeMinutes() != null) {
            availableMaximum += 10;
        }

        double raw = history.score() + savedStyleScore + preferredTagScore
                + hair.score() + careTimeScore;
        double finalScore = availableMaximum == 0 ? 0 : raw / availableMaximum * 100;
        if (availableMaximum == 0) {
            reasons.add(new RecommendationReason(
                    RecommendationReason.Code.EDITORIAL_FALLBACK, Map.of()));
        }
        ScoreBreakdown breakdown = new ScoreBreakdown(
                history.score(), savedStyleScore, preferredTagScore, hair.score(), careTimeScore,
                raw, availableMaximum, clamp(finalScore));
        return Optional.of(new ScoredRecommendation(
                candidate, breakdown, reasons, history.referenceRecordId()));
    }

    private boolean eligible(HairstyleCandidate candidate, RecommendationContext context) {
        if (!candidate.active() || candidate.thumbnailFileId() == null
                || candidate.assetVersion().isBlank() || candidate.metadataVersion().isBlank()) {
            return false;
        }
        if (!disjoint(candidate.tags().keySet(), context.excludedTagIds())) {
            return false;
        }
        HairProfile profile = context.hairProfile();
        if (profile == null) {
            return true;
        }
        if (candidate.minimumHairLength() != null && profile.hairLength() != null
                && profile.hairLength().ordinal() < candidate.minimumHairLength().ordinal()) {
            return false;
        }
        return profile.hairCondition() == null
                || !candidate.contraindicatedHairConditions().contains(profile.hairCondition());
    }

    private HistoryResult historyScore(
            HairstyleCandidate candidate,
            RecommendationContext context,
            List<RecommendationReason> reasons
    ) {
        double best = 0;
        TreatmentRecord bestRecord = null;
        for (TreatmentRecord record : context.recentTreatments()) {
            if (record.satisfaction() == null) {
                continue;
            }
            double similarity = jaccard(candidate.serviceTypes(), record.serviceTypes());
            double satisfaction = (record.satisfaction() - 1.0) / 4.0;
            long daysAgo = Math.max(0, Duration.between(
                    record.performedAt(), context.generatedAt()).toDays());
            double recency = Math.pow(0.5, daysAgo / 180.0);
            double score = 40 * similarity * satisfaction * recency;
            if (score > best || (score == best && bestRecord != null
                    && record.recordId().compareTo(bestRecord.recordId()) < 0)) {
                best = score;
                bestRecord = record;
            }
        }
        if (bestRecord != null && best > 0) {
            reasons.add(new RecommendationReason(
                    RecommendationReason.Code.SIMILAR_HIGH_SATISFACTION_HISTORY,
                    Map.of("record_id", bestRecord.recordId().toString(),
                            "satisfaction", bestRecord.satisfaction().toString())));
        }
        return new HistoryResult(best, bestRecord == null ? null : bestRecord.recordId());
    }

    private double savedStyleScore(
            HairstyleCandidate candidate,
            RecommendationContext context,
            List<RecommendationReason> reasons
    ) {
        if (!context.savedHairstyleIds().contains(candidate.hairstyleId())) {
            return 0;
        }
        reasons.add(new RecommendationReason(
                RecommendationReason.Code.SAVED_STYLE_MATCH, Map.of()));
        return 15;
    }

    private double preferredTagScore(
            HairstyleCandidate candidate,
            RecommendationContext context,
            List<RecommendationReason> reasons
    ) {
        if (candidate.tags().isEmpty()) {
            return 0;
        }
        List<UUID> matches = candidate.tags().keySet().stream()
                .filter(context.preferredTagIds()::contains)
                .sorted()
                .toList();
        if (matches.isEmpty()) {
            return 0;
        }
        UUID representative = matches.getFirst();
        reasons.add(new RecommendationReason(
                RecommendationReason.Code.PREFERRED_TAG_MATCH,
                Map.of("tag_id", representative.toString(),
                        "tag_name", candidate.tags().get(representative))));
        return 15.0 * matches.size() / candidate.tags().size();
    }

    private HairResult hairScore(
            HairstyleCandidate candidate,
            RecommendationContext context,
            List<RecommendationReason> reasons
    ) {
        HairProfile profile = context.hairProfile();
        if (profile == null) {
            return new HairResult(0, 0);
        }
        double score = 0;
        double maximum = 0;
        if (profile.hairLength() != null) {
            maximum += 8;
            score += candidate.compatibleHairLengths().contains(profile.hairLength()) ? 8 : 0;
        }
        if (profile.hairType() != null) {
            maximum += 4;
            score += candidate.compatibleHairTypes().contains(profile.hairType()) ? 4 : 0;
        }
        if (profile.hairThickness() != null) {
            maximum += 4;
            score += candidate.compatibleHairThicknesses().contains(profile.hairThickness()) ? 4 : 0;
        }
        if (profile.hairCondition() != null) {
            maximum += 4;
            score += candidate.compatibleHairConditions().contains(profile.hairCondition()) ? 4 : 0;
        }
        if (score > 0) {
            reasons.add(new RecommendationReason(
                    RecommendationReason.Code.HAIR_PROFILE_COMPATIBLE,
                    Map.of("matched_score", number(score))));
        }
        return new HairResult(score, maximum);
    }

    private double careTimeScore(
            HairstyleCandidate candidate,
            RecommendationContext context,
            List<RecommendationReason> reasons
    ) {
        HairProfile profile = context.hairProfile();
        if (profile == null || profile.availableCareTimeMinutes() == null) {
            return 0;
        }
        int required = candidate.estimatedDailyCareMinutes();
        double score = required == 0 ? 10
                : 10 * Math.min(1.0, profile.availableCareTimeMinutes() / (double) required);
        if (profile.availableCareTimeMinutes() >= required) {
            reasons.add(new RecommendationReason(
                    RecommendationReason.Code.CARE_TIME_FIT,
                    Map.of("required_minutes", Integer.toString(required),
                            "available_minutes", profile.availableCareTimeMinutes().toString())));
        }
        return score;
    }

    private Comparator<ScoredRecommendation> scoreOrder() {
        return Comparator.comparingDouble(ScoredRecommendation::finalScore).reversed()
                .thenComparing(Comparator.comparingInt(
                        (ScoredRecommendation value) -> value.candidate().editorialPriority()).reversed())
                .thenComparing(value -> value.candidate().hairstyleId());
    }

    private static boolean disjoint(Set<UUID> first, Set<UUID> second) {
        return first.stream().noneMatch(second::contains);
    }

    private static double jaccard(Set<?> first, Set<?> second) {
        if (first.isEmpty() && second.isEmpty()) {
            return 0;
        }
        Set<Object> union = new HashSet<>(first);
        union.addAll(second);
        Set<Object> intersection = new HashSet<>(first);
        intersection.retainAll(second);
        return intersection.size() / (double) union.size();
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String number(double value) {
        return Double.toString(Math.round(value * 100.0) / 100.0);
    }

    private record HistoryResult(double score, UUID referenceRecordId) { }
    private record HairResult(double score, double availableMaximum) { }
}
