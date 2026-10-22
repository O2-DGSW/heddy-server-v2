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

    @Test
    void prioritizesUnseenStylesEvenWhenTheirCategoryAndTagMatch() {
        UUID sameTag = UUID.randomUUID();
        var seen = item("B", UUID.randomUUID(), 100);
        var unseen = List.of(item("A", sameTag, 90), item("A", sameTag, 80), item("A", sameTag, 70));
        var candidates = new java.util.ArrayList<>(List.of(seen));
        candidates.addAll(unseen);

        assertThat(diversifier.topThree(candidates, Map.of(seen.candidate().hairstyleId(), 1L)))
                .containsExactlyElementsOf(unseen);
    }

    @Test
    void fillsFromLeastExposedStylesWithoutDuplicatesWhenUnseenStylesRunOut() {
        var frequent = item("A", UUID.randomUUID(), 100);
        var rare = item("B", UUID.randomUUID(), 90);
        var unseen = item("C", UUID.randomUUID(), 80);
        var counts = Map.of(frequent.candidate().hairstyleId(), 4L, rare.candidate().hairstyleId(), 1L);

        assertThat(diversifier.topThree(List.of(frequent, rare, unseen), counts))
                .containsExactly(unseen, rare, frequent);
        assertThat(diversifier.topThree(List.of(frequent), counts)).containsExactly(frequent);
    }

    @Test
    void preferencesSurviveRefreshAndDiversityLimits() {
        UUID tag = UUID.randomUUID();
        var preferred = List.of(preferredItem("A", tag, 90), preferredItem("A", tag, 80),
                preferredItem("A", tag, 70));
        var unrelated = item("B", UUID.randomUUID(), 100);
        var candidates = new java.util.ArrayList<>(List.of(unrelated));
        candidates.addAll(preferred);
        var exposures = new java.util.HashMap<UUID, Long>();
        preferred.forEach(item -> exposures.put(item.candidate().hairstyleId(), 5L));
        assertThat(diversifier.topThree(candidates, exposures)).containsExactlyElementsOf(preferred);
        assertThat(diversifier.topThree(List.of(unrelated, preferred.getFirst()), exposures))
                .containsExactly(preferred.getFirst(), unrelated);
    }

    private ScoredRecommendation preferredItem(String category, UUID tag, double score) {
        var original = item(category, tag, score);
        return new ScoredRecommendation(original.candidate(),
                new ScoreBreakdown(0, 0, 15, 0, 0, 15, 100, score), List.of(), null);
    }

    private ScoredRecommendation item(String category, UUID tagId, double score) {
        HairstyleCandidate candidate = new HairstyleCandidate(UUID.randomUUID(), "style", category,
                UUID.randomUUID(), "NONE", true, "1", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                Set.of(), null, 0, ManagementDifficulty.LOW, ChemicalStressLevel.LOW,
                0, "1", Map.of(tagId, "tag"));
        ScoreBreakdown breakdown = new ScoreBreakdown(0, 0, 0, 0, 0, 0, 100, score);
        return new ScoredRecommendation(candidate, breakdown, List.of(), null);
    }
}
