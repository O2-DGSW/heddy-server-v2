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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

        assertThat(exact).isEqualTo(10);
        assertThat(partial).isEqualTo(5);
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
        assertThat(recentFive).isEqualTo(10);
        assertThat(oldFive).isCloseTo(5, org.assertj.core.data.Offset.offset(0.0001));
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
                UUID.randomUUID(), "NONE", true, "1", Set.of(), Set.of(HairLength.BELOW_CHEST),
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
    void highHistoryAndPreferenceScoresCannotOverrideMissingMinimumLength() {
        UUID tag = UUID.randomUUID();
        HairstyleCandidate medium = candidate(Set.of(ServiceType.CUT, ServiceType.PERM), Map.of(tag, "선호"));
        RecommendationContext shortHair = context(
                profile(HairLength.SHORT, HairType.WAVY, HairThickness.NORMAL, HairCondition.HEALTHY, 15),
                Set.of(tag), Set.of(), Set.of(medium.hairstyleId()),
                List.of(treatment(Set.of(ServiceType.CUT, ServiceType.PERM), 5, 0)));

        assertThat(scorer.score(medium, shortHair)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(HairLength.class)
    void inferredMinimumAllowsOnlySufficientLength(HairLength length) {
        HairstyleCandidate medium = withLengths(null, Set.of(HairLength.BELOW_CHIN, HairLength.BELOW_SHOULDER));
        var result = scorer.score(medium, context(profile(length, null, null, null, null),
                Set.of(), Set.of(), Set.of(), List.of()));

        assertThat(result.isPresent()).isEqualTo(length != HairLength.SHORT);
    }

    @Test
    void stricterOfExplicitAndCompatibleMinimumWins() {
        var chinLength = context(profile(HairLength.BELOW_CHIN, null, null, null, null),
                Set.of(), Set.of(), Set.of(), List.of());
        assertThat(scorer.score(withLengths(HairLength.BELOW_CHIN,
                Set.of(HairLength.BELOW_SHOULDER, HairLength.BELOW_CHEST)), chinLength)).isEmpty();
        assertThat(scorer.score(withLengths(HairLength.BELOW_SHOULDER,
                Set.of(HairLength.SHORT, HairLength.BELOW_CHIN)), chinLength)).isEmpty();
    }

    @Test
    void unknownCatalogLengthIsNotAssumedSuitableForKnownUserLength() {
        var shortHair = context(profile(HairLength.SHORT, null, null, null, null),
                Set.of(), Set.of(), Set.of(), List.of());
        assertThat(scorer.score(withLengths(null, Set.of()), shortHair)).isEmpty();
        assertThat(scorer.score(withLengths(HairLength.SHORT, Set.of()), shortHair)).isPresent();
        assertThat(scorer.score(withLengths(null, Set.of()),
                context(null, Set.of(), Set.of(), Set.of(), List.of()))).isPresent();
    }

    @Test
    void longerHairCanStillChooseShortCut() {
        assertThat(scorer.score(withLengths(null, Set.of(HairLength.SHORT)),
                context(profile(HairLength.BELOW_CHEST, null, null, null, null),
                        Set.of(), Set.of(), Set.of(), List.of()))).isPresent();
    }

    private HairstyleCandidate withLengths(HairLength minimum, Set<HairLength> lengths) {
        return new HairstyleCandidate(UUID.randomUUID(), "style", "CUT", UUID.randomUUID(),
                "NONE", true, "1", Set.of(), lengths, Set.of(), Set.of(), Set.of(), Set.of(),
                minimum, 0, ManagementDifficulty.LOW, ChemicalStressLevel.LOW, 1, "1", Map.of());
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

    @Test
    void changingCutShapeChangesTheWinnerEvenWithIdenticalServiceTypes() {
        var layered = named("레이어드 C컬", "MEDIUM", ServiceType.CUT);
        var buzz = named("버즈컷", "SHORT", ServiceType.CUT);
        var history = detailed("LAYERED", null, null, 5);
        var first = scorer.scoreAll(List.of(buzz, layered), context(null, Set.of(), Set.of(), Set.of(), List.of(history)));
        assertThat(first.getFirst().candidate()).isEqualTo(layered);
        assertThat(first.getFirst().scoreBreakdown().historyScore()).isEqualTo(40);
        assertThat(first.getFirst().reasons().getFirst().params().get("matched_details")).contains("레이어드");
        var changed = detailed("BUZZ", null, null, 5);
        assertThat(scorer.scoreAll(List.of(layered, buzz), context(null, Set.of(), Set.of(), Set.of(), List.of(changed)))
                .getFirst().candidate()).isEqualTo(buzz);
    }

    @Test
    void lowSatisfactionDoesNotProducePositiveHistoryReasonOrReference() {
        var style = named("레이어드 C컬", "MEDIUM", ServiceType.CUT);
        var result = scorer.score(style, context(null, Set.of(), Set.of(), Set.of(),
                List.of(detailed("LAYERED", null, null, 2)))).orElseThrow();
        assertThat(result.referenceRecordId()).isNull();
        assertThat(result.scoreBreakdown().historyScore()).isNegative();
        assertThat(result.finalScore()).isZero();
        assertThat(result.reasons()).noneMatch(r -> r.code().name().contains("HISTORY"));
    }

    @Test
    void genericServiceMatchDoesNotClaimSimilarStyle() {
        var result = scorer.score(candidate(Set.of(ServiceType.CUT), Map.of()), context(null,
                Set.of(), Set.of(), Set.of(), List.of(treatment(Set.of(ServiceType.CUT), 5, 0)))).orElseThrow();
        assertThat(result.reasons()).extracting(r -> r.code().name()).containsExactly("HISTORY_SERVICE_MATCH");
    }

    @Test
    void lengthAndPermDetailsDistinguishCandidatesAndUnknownDetailsDoNotMatch() {
        var shortCut = named("버즈컷", "SHORT", ServiceType.CUT);
        var medium = named("레이어드 C컬", "MEDIUM", ServiceType.CUT);
        var length = detailed(null, "MEDIUM", null, 5);
        assertThat(scorer.scoreAll(List.of(shortCut, medium), context(null, Set.of(), Set.of(), Set.of(), List.of(length)))
                .getFirst().candidate()).isEqualTo(medium);
        var wave = named("내추럴 웨이브", "MEDIUM", ServiceType.PERM);
        var cCurl = named("레이어드 C컬", "MEDIUM", ServiceType.PERM);
        var perm = detailed(null, null, "C_CURL", 5);
        assertThat(scorer.scoreAll(List.of(wave, cCurl), context(null, Set.of(), Set.of(), Set.of(), List.of(perm)))
                .getFirst().candidate()).isEqualTo(cCurl);
        var unknown = detailed("UNKNOWN", null, null, 5);
        assertThat(scorer.score(medium, context(null, Set.of(), Set.of(), Set.of(), List.of(unknown)))
                .orElseThrow().scoreBreakdown().historyScore()).isEqualTo(10);
    }

    @Test
    void photoShapeChangesRankingWithinTheSameHairLength() {
        var fringe = named("사이드 프린지", "SHORT", ServiceType.CUT);
        var buzz = named("버즈컷", "SHORT", ServiceType.CUT);
        var profile = profile(HairLength.SHORT, null, null, null, null);
        var firstPhoto = new RecommendationContext(profile, Set.of(), Set.of(), Set.of(), List.of(), NOW,
                Map.of(fringe.hairstyleId(), .85, buzz.hairstyleId(), .15));
        var secondPhoto = new RecommendationContext(profile, Set.of(), Set.of(), Set.of(), List.of(), NOW,
                Map.of(fringe.hairstyleId(), .20, buzz.hairstyleId(), .90));
        var first = scorer.scoreAll(List.of(buzz, fringe), firstPhoto).getFirst();
        assertThat(first.candidate()).isEqualTo(fringe);
        assertThat(first.reasons().getFirst().code().name()).isEqualTo("PHOTO_SHAPE_MATCH");
        assertThat(scorer.scoreAll(List.of(fringe, buzz), secondPhoto).getFirst().candidate()).isEqualTo(buzz);
        // Incompatible lengths remain excluded even with a perfect visual match.
        var medium = candidate(Set.of(ServiceType.CUT), Map.of());
        assertThat(scorer.score(medium, new RecommendationContext(profile, Set.of(), Set.of(), Set.of(), List.of(), NOW,
                Map.of(medium.hairstyleId(), 1.0)))).isEmpty();
    }

    private TreatmentRecord detailed(String shape, String length, String perm, int satisfaction) {
        return new TreatmentRecord(UUID.randomUUID(), UUID.randomUUID(),
                Set.of(perm == null ? ServiceType.CUT : ServiceType.PERM), null, null, NOW, satisfaction,
                null, null, null, null, null, null, null, "Asia/Seoul", length, shape, perm, null,
                List.of(), List.of(), NOW);
    }

    private HairstyleCandidate named(String name, String category, ServiceType service) {
        return new HairstyleCandidate(UUID.randomUUID(), name, category, UUID.randomUUID(), "NONE", true, "1",
                Set.of(service), Set.of(HairLength.SHORT), Set.of(), Set.of(), Set.of(), Set.of(),
                HairLength.SHORT, 0, ManagementDifficulty.LOW, ChemicalStressLevel.LOW, 1, "1", Map.of());
    }

    private RecommendationContext context(
            HairProfile profile, Set<UUID> preferred, Set<UUID> excluded,
            Set<UUID> saved, List<TreatmentRecord> treatments
    ) {
        return new RecommendationContext(profile, preferred, excluded, saved, treatments, NOW);
    }

    private HairstyleCandidate candidate(Set<ServiceType> services, Map<UUID, String> tags) {
        return new HairstyleCandidate(UUID.randomUUID(), "레이어드 C컬", "MEDIUM",
                UUID.randomUUID(), "NONE", true, "1.0.0", services,
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
