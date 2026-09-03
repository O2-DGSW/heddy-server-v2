package com.heddy.domain.recommendation.service;

import com.heddy.domain.recommendation.model.ScoredRecommendation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 정렬된 후보에서 카테고리·대표 태그 편중을 결정론적으로 줄인다. */
public final class RecommendationDiversifier {

    public List<ScoredRecommendation> topThree(List<ScoredRecommendation> orderedCandidates) {
        List<ScoredRecommendation> selected = new ArrayList<>(3);
        addEligible(orderedCandidates, selected, true, true);
        addEligible(orderedCandidates, selected, true, false);
        addEligible(orderedCandidates, selected, false, false);
        return List.copyOf(selected);
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
