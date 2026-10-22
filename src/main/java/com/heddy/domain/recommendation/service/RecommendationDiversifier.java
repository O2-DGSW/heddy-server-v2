package com.heddy.domain.recommendation.service;

import com.heddy.domain.recommendation.model.ScoredRecommendation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/** 정렬된 후보에서 카테고리·대표 태그 편중을 결정론적으로 줄인다. */
public final class RecommendationDiversifier {

    public List<ScoredRecommendation> topThree(List<ScoredRecommendation> orderedCandidates) {
        return topThree(orderedCandidates, Map.of());
    }

    /** 새로고침은 미노출/저노출 후보부터 선택하고 같은 노출 횟수 안에서는 기존 순위를 유지한다. */
    public List<ScoredRecommendation> topThree(
            List<ScoredRecommendation> orderedCandidates, Map<UUID, Long> exposureCounts
    ) {
        List<ScoredRecommendation> selected = new ArrayList<>(3);
        // Explicit preferences take priority over variety and refresh exposure counts.
        // Nonmatching styles only fill remaining slots when matching candidates are insufficient.
        Map<Tier, List<ScoredRecommendation>> tiers = new TreeMap<>();
        for (ScoredRecommendation candidate : orderedCandidates) {
            long exposures = exposureCounts.getOrDefault(candidate.candidate().hairstyleId(), 0L);
            Tier tier = new Tier(candidate.scoreBreakdown().preferredTagScore() > 0 ? 0 : 1, exposures);
            tiers.computeIfAbsent(tier, ignored -> new ArrayList<>()).add(candidate);
        }
        for (List<ScoredRecommendation> tier : tiers.values()) {
            addEligible(tier, selected, true, true);
            addEligible(tier, selected, true, false);
            addEligible(tier, selected, false, false);
            if (selected.size() == 3) break;
        }
        return List.copyOf(selected);
    }

    private record Tier(int preferenceOrder, long exposures) implements Comparable<Tier> {
        public int compareTo(Tier other) {
            int preference = Integer.compare(preferenceOrder, other.preferenceOrder);
            return preference != 0 ? preference : Long.compare(exposures, other.exposures);
        }
    }

    private void addEligible(
            List<ScoredRecommendation> candidates,
            List<ScoredRecommendation> selected,
            boolean categoryLimit,
            boolean representativeTagLimit
    ) {
        if (selected.size() >= 3) {
            return;
        }
        Map<String, Integer> categories = new HashMap<>();
        Set<UUID> representativeTags = new HashSet<>();
        for (ScoredRecommendation item : selected) {
            categories.merge(item.candidate().category(), 1, Integer::sum);
            UUID tagId = item.candidate().representativeTagId();
            if (tagId != null) {
                representativeTags.add(tagId);
            }
        }
        for (ScoredRecommendation candidate : candidates) {
            if (selected.size() >= 3) {
                return;
            }
            if (selected.contains(candidate)) {
                continue;
            }
            if (categoryLimit
                    && categories.getOrDefault(candidate.candidate().category(), 0) >= 2) {
                continue;
            }
            UUID tagId = candidate.candidate().representativeTagId();
            if (representativeTagLimit && tagId != null && representativeTags.contains(tagId)) {
                continue;
            }
            selected.add(candidate);
            categories.merge(candidate.candidate().category(), 1, Integer::sum);
            if (tagId != null) {
                representativeTags.add(tagId);
            }
        }
    }
}
