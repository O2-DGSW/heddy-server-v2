package com.heddy.domain.recommendation.service;

import com.heddy.domain.recommendation.model.HairstyleCandidate;
import com.heddy.domain.recommendation.model.HairstyleCandidate.ChemicalStressLevel;
import com.heddy.domain.recommendation.model.HairstyleCandidate.ManagementDifficulty;
import com.heddy.domain.recommendation.model.ScoreBreakdown;
import com.heddy.domain.recommendation.model.ScoredRecommendation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationDiversifierTest {
    private final RecommendationDiversifier diversifier = new RecommendationDiversifier();

    @Test
    void limitsSameCategoryToTwoWhenAlternativesExist() {
        List<ScoredRecommendation> result = diversifier.topThree(List.of(
                item("A", UUID.randomUUID(), 100), item("A", UUID.randomUUID(), 90),
                item("A", UUID.randomUUID(), 80), item("B", UUID.randomUUID(), 70)));

        assertThat(result).extracting(value -> value.candidate().category())
                .containsExactly("A", "A", "B");
    }

    @Test
    void relaxesLimitsWhenCandidatesAreInsufficientAndIsDeterministic() {
        UUID sameTag = UUID.randomUUID();
        List<ScoredRecommendation> candidates = List.of(
                item("A", sameTag, 100), item("A", sameTag, 90), item("A", sameTag, 80));

        assertThat(diversifier.topThree(candidates)).containsExactlyElementsOf(candidates);
        assertThat(diversifier.topThree(candidates)).isEqualTo(diversifier.topThree(candidates));
    }

    private ScoredRecommendation item(String category, UUID tagId, double score) {
        HairstyleCandidate candidate = new HairstyleCandidate(UUID.randomUUID(), "style", category,
                UUID.randomUUID(), true, "1", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                Set.of(), null, 0, ManagementDifficulty.LOW, ChemicalStressLevel.LOW,
                0, "1", Map.of(tagId, "tag"));
        ScoreBreakdown breakdown = new ScoreBreakdown(0, 0, 0, 0, 0, 0, 100, score);
        return new ScoredRecommendation(candidate, breakdown, List.of(), null);
    }
}
