package com.heddy.domain.recommendation.service;

import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.account.model.HairProfile.HairCondition;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.account.model.HairProfile.HairThickness;
import com.heddy.domain.account.model.HairProfile.HairType;
import com.heddy.domain.recommendation.model.HairstyleCandidate;
import com.heddy.domain.recommendation.model.HairstyleCandidate.ChemicalStressLevel;
import com.heddy.domain.recommendation.model.HairstyleCandidate.ManagementDifficulty;
import com.heddy.domain.recommendation.model.RecommendationContext;
import com.heddy.domain.recommendation.model.ScoredRecommendation;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.model.TreatmentRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedV1ScorerTest {
    private static final Instant NOW = Instant.parse("2026-09-01T00:00:00Z");
    private final RuleBasedV1Scorer scorer = new RuleBasedV1Scorer();

    @Test
    void exactServiceMatchScoresHigherThanPartialMatch() {
        TreatmentRecord record = treatment(Set.of(ServiceType.CUT, ServiceType.PERM), 5, 0);
        RecommendationContext context = context(null, Set.of(), Set.of(), Set.of(), List.of(record));

        double exact = scorer.score(candidate(Set.of(ServiceType.CUT, ServiceType.PERM), Map.of()), context)
                .orElseThrow().scoreBreakdown().historyScore();
        double partial = scorer.score(candidate(Set.of(ServiceType.CUT), Map.of()), context)
                .orElseThrow().scoreBreakdown().historyScore();

        assertThat(exact).isEqualTo(40);
        assertThat(partial).isEqualTo(20);
    }

    @Test
    void satisfactionAndRecencyApplyToHistoryScore() {
        HairstyleCandidate candidate = candidate(Set.of(ServiceType.CUT), Map.of());
        double onePoint = scorer.score(candidate, context(null, Set.of(), Set.of(), Set.of(),
                        List.of(treatment(Set.of(ServiceType.CUT), 1, 0))))
                .orElseThrow().scoreBreakdown().historyScore();
        double recentFive = scorer.score(candidate, context(null, Set.of(), Set.of(), Set.of(),
                        List.of(treatment(Set.of(ServiceType.CUT), 5, 0))))
                .orElseThrow().scoreBreakdown().historyScore();
        double oldFive = scorer.score(candidate, context(null, Set.of(), Set.of(), Set.of(),
                        List.of(treatment(Set.of(ServiceType.CUT), 5, 180))))
                .orElseThrow().scoreBreakdown().historyScore();

        assertThat(onePoint).isZero();
        assertThat(recentFive).isEqualTo(40);
        assertThat(oldFive).isCloseTo(20, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void savedStylePreferredTagHairAndCareScoresAreCombined() {
        UUID preferredTag = UUID.randomUUID();
        HairstyleCandidate candidate = candidate(Set.of(), Map.of(
                preferredTag, "레이어드", UUID.randomUUID(), "내추럴"));
        HairProfile profile = profile(HairLength.BELOW_SHOULDER, HairType.WAVY,
                HairThickness.NORMAL, HairCondition.HEALTHY, 15);
        RecommendationContext context = context(profile, Set.of(preferredTag), Set.of(),
                Set.of(candidate.hairstyleId()), List.of());

        var result = scorer.score(candidate, context).orElseThrow().scoreBreakdown();

        assertThat(result.savedStyleScore()).isEqualTo(15);
        assertThat(result.preferredTagScore()).isEqualTo(7.5);
        assertThat(result.hairCompatibilityScore()).isEqualTo(20);
        assertThat(result.careTimeScore()).isEqualTo(10);
        assertThat(result.finalScore()).isEqualTo(87.5);
    }

    @Test
    void missingInputsAreRemovedFromDenominator() {
        HairProfile onlyLength = profile(HairLength.BELOW_SHOULDER, null, null, null, null);
        var result = scorer.score(candidate(Set.of(), Map.of()),
                context(onlyLength, Set.of(), Set.of(), Set.of(), List.of())).orElseThrow();

        assertThat(result.scoreBreakdown().rawScore()).isEqualTo(8);
        assertThat(result.scoreBreakdown().availableMaximumScore()).isEqualTo(8);
        assertThat(result.finalScore()).isEqualTo(100);
    }

    @Test
    void excludedTagMinimumLengthAndContraindicationAreHardFilters() {
        UUID excluded = UUID.randomUUID();
        HairstyleCandidate tagged = candidate(Set.of(), Map.of(excluded, "제외"));
        assertThat(scorer.score(tagged,
                context(null, Set.of(), Set.of(excluded), Set.of(), List.of()))).isEmpty();

        HairstyleCandidate longOnly = new HairstyleCandidate(tagged.hairstyleId(), "style", "CUT",
                UUID.randomUUID(), true, "1", Set.of(), Set.of(HairLength.BELOW_CHEST),
                Set.of(HairType.WAVY), Set.of(HairThickness.NORMAL), Set.of(HairCondition.HEALTHY),
                Set.of(HairCondition.DAMAGED), HairLength.BELOW_CHEST, 10,
                ManagementDifficulty.MEDIUM, ChemicalStressLevel.LOW, 1, "1", Map.of());
        assertThat(scorer.score(longOnly, context(
                profile(HairLength.SHORT, HairType.WAVY, HairThickness.NORMAL,
                        HairCondition.HEALTHY, 10), Set.of(), Set.of(), Set.of(), List.of()))).isEmpty();
        assertThat(scorer.score(longOnly, context(
                profile(HairLength.BELOW_CHEST, HairType.WAVY, HairThickness.NORMAL,
                        HairCondition.DAMAGED, 10), Set.of(), Set.of(), Set.of(), List.of()))).isEmpty();
    }

    @Test
    void coldStartUsesZeroPersonalizedScore() {
        RecommendationContext context = context(null, Set.of(), Set.of(), Set.of(), List.of());
        ScoredRecommendation result = scorer.score(candidate(Set.of(), Map.of()), context).orElseThrow();

        assertThat(context.coldStart()).isTrue();
        assertThat(result.finalScore()).isZero();
        assertThat(result.reasons()).extracting(reason -> reason.code().name())
                .containsExactly("EDITORIAL_FALLBACK");
    }

    private RecommendationContext context(
            HairProfile profile, Set<UUID> preferred, Set<UUID> excluded,
            Set<UUID> saved, List<TreatmentRecord> treatments
    ) {
        return new RecommendationContext(profile, preferred, excluded, saved, treatments, NOW);
    }

    private HairstyleCandidate candidate(Set<ServiceType> services, Map<UUID, String> tags) {
        return new HairstyleCandidate(UUID.randomUUID(), "레이어드 C컬", "MEDIUM",
                UUID.randomUUID(), true, "1.0.0", services,
                Set.of(HairLength.BELOW_SHOULDER), Set.of(HairType.WAVY),
                Set.of(HairThickness.NORMAL), Set.of(HairCondition.HEALTHY), Set.of(), null,
                10, ManagementDifficulty.MEDIUM, ChemicalStressLevel.LOW, 10, "1", tags);
    }

    private HairProfile profile(
            HairLength length, HairType type, HairThickness thickness,
            HairCondition condition, Integer careMinutes
    ) {
        return new HairProfile(UUID.randomUUID(), UUID.randomUUID(), type, condition, length,
                thickness, careMinutes, null, null);
    }

    private TreatmentRecord treatment(Set<ServiceType> services, int satisfaction, int daysAgo) {
        return new TreatmentRecord(UUID.randomUUID(), UUID.randomUUID(), services, null, null,
                NOW.minus(daysAgo, ChronoUnit.DAYS), satisfaction, null, null, null,
                null, null, List.of(), NOW.minus(daysAgo, ChronoUnit.DAYS));
    }
}
