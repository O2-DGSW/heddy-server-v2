package com.heddy.application.recommendation.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.HairProfileRepositoryPort;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import com.heddy.domain.recommendation.exception.RecommendationError;
import com.heddy.domain.recommendation.exception.RecommendationException;
import com.heddy.domain.recommendation.model.HairstyleCandidate;
import com.heddy.domain.recommendation.model.RecommendationContext;
import com.heddy.domain.recommendation.model.RecommendationBasis;
import com.heddy.domain.recommendation.model.RecommendationItem;
import com.heddy.domain.recommendation.model.RecommendationReason;
import com.heddy.domain.recommendation.model.RecommendationReference;
import com.heddy.domain.recommendation.model.RecommendationRun;
import com.heddy.domain.recommendation.model.ScoredRecommendation;
import com.heddy.domain.recommendation.port.in.GenerateRecommendationUseCase;
import com.heddy.domain.recommendation.port.in.GetLatestRecommendationUseCase;
import com.heddy.domain.recommendation.port.in.GetRecommendationUseCase;
import com.heddy.domain.recommendation.port.in.RecommendationResult;
import com.heddy.domain.recommendation.port.out.HairstyleCatalogRepositoryPort;
import com.heddy.domain.recommendation.port.out.RecommendationRepositoryPort;
import com.heddy.domain.recommendation.port.out.RecommendationExplanationPort;
import com.heddy.domain.recommendation.service.RecommendationDiversifier;
import com.heddy.domain.recommendation.service.RuleBasedV1Scorer;
import com.heddy.domain.style.model.UserStylePreference;
import com.heddy.domain.style.port.out.SavedStyleRepositoryPort;
import com.heddy.domain.style.port.out.UserStylePreferenceRepositoryPort;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.model.TreatmentHistorySummary;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RecommendationService implements GenerateRecommendationUseCase,
        GetLatestRecommendationUseCase, GetRecommendationUseCase {
    private final AccountRepositoryPort accountRepositoryPort;
    private final HairProfileRepositoryPort hairProfileRepositoryPort;
    private final UserStylePreferenceRepositoryPort preferenceRepositoryPort;
    private final SavedStyleRepositoryPort savedStyleRepositoryPort;
    private final TreatmentRecordRepositoryPort treatmentRepositoryPort;
    private final HairstyleCatalogRepositoryPort catalogRepositoryPort;
    private final RecommendationRepositoryPort recommendationRepositoryPort;
    private final FileRepositoryPort fileRepositoryPort;
    private final FileStoragePort fileStoragePort;
    private final RecommendationExplanationPort explanationPort;
    private final RuleBasedV1Scorer scorer;
    private final RecommendationDiversifier diversifier;
    private final Clock clock;
    private final RecommendationPhotoResolver photoResolver;

    @Autowired
    public RecommendationService(
            AccountRepositoryPort accountRepositoryPort,
            HairProfileRepositoryPort hairProfileRepositoryPort,
            UserStylePreferenceRepositoryPort preferenceRepositoryPort,
            SavedStyleRepositoryPort savedStyleRepositoryPort,
            TreatmentRecordRepositoryPort treatmentRepositoryPort,
            HairstyleCatalogRepositoryPort catalogRepositoryPort,
            RecommendationRepositoryPort recommendationRepositoryPort,
            FileRepositoryPort fileRepositoryPort,
            FileStoragePort fileStoragePort,
            RecommendationExplanationPort explanationPort,
            RecommendationPhotoResolver photoResolver
    ) {
        this(accountRepositoryPort, hairProfileRepositoryPort, preferenceRepositoryPort,
                savedStyleRepositoryPort, treatmentRepositoryPort, catalogRepositoryPort,
                recommendationRepositoryPort, fileRepositoryPort, fileStoragePort, explanationPort,
                new RuleBasedV1Scorer(), new RecommendationDiversifier(), Clock.systemUTC(), photoResolver);
    }

    RecommendationService(
            AccountRepositoryPort accountRepositoryPort,
            HairProfileRepositoryPort hairProfileRepositoryPort,
            UserStylePreferenceRepositoryPort preferenceRepositoryPort,
            SavedStyleRepositoryPort savedStyleRepositoryPort,
            TreatmentRecordRepositoryPort treatmentRepositoryPort,
            HairstyleCatalogRepositoryPort catalogRepositoryPort,
            RecommendationRepositoryPort recommendationRepositoryPort,
            FileRepositoryPort fileRepositoryPort,
            FileStoragePort fileStoragePort,
            RecommendationExplanationPort explanationPort,
            RuleBasedV1Scorer scorer,
            RecommendationDiversifier diversifier,
            Clock clock,
            RecommendationPhotoResolver photoResolver
    ) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.hairProfileRepositoryPort = hairProfileRepositoryPort;
        this.preferenceRepositoryPort = preferenceRepositoryPort;
        this.savedStyleRepositoryPort = savedStyleRepositoryPort;
        this.treatmentRepositoryPort = treatmentRepositoryPort;
        this.catalogRepositoryPort = catalogRepositoryPort;
        this.recommendationRepositoryPort = recommendationRepositoryPort;
        this.fileRepositoryPort = fileRepositoryPort;
        this.fileStoragePort = fileStoragePort;
        this.explanationPort = explanationPort;
        this.scorer = scorer;
        this.diversifier = diversifier;
        this.clock = clock;
        this.photoResolver = photoResolver;
    }

    @Override
    @Transactional
    public RecommendationResult generate(UUID userId, boolean forceRefresh) {
        lockActiveAccount(userId);
        return generateForLockedAccount(userId, forceRefresh);
    }

    private void lockActiveAccount(UUID userId) {
        validateActiveAccount(accountRepositoryPort.findByIdForUpdate(userId)
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND)));
    }

    private RecommendationResult generateForLockedAccount(UUID userId, boolean forceRefresh) {
        Instant generatedAt = clock.instant();
        HairProfile profile = hairProfileRepositoryPort.findByUserId(userId).orElse(null);
        List<UserStylePreference> preferences = preferenceRepositoryPort.findAllByUserId(userId);
        Set<UUID> preferred = preferenceIds(preferences, UserStylePreference.PreferenceType.PREFERRED);
        Set<UUID> excluded = preferenceIds(preferences, UserStylePreference.PreferenceType.EXCLUDED);
        Set<UUID> saved = Set.copyOf(savedStyleRepositoryPort.findHairstyleIdsByUserId(userId));
        List<TreatmentRecord> treatments = treatmentRepositoryPort.findRecentByUserId(userId, 10);
        var photo = photoResolver.resolve(userId, profile, treatments);
        profile = photo.profile();
        if (profile == null || profile.hairLength() == null) {
            throw new RecommendationException(RecommendationError.HAIR_LENGTH_REQUIRED);
        }
        TreatmentHistorySummary treatmentHistory = treatmentRepositoryPort.summarizeByUserId(userId);
        List<HairstyleCandidate> candidates = catalogRepositoryPort.findEligibleCandidates();
        RecommendationContext context = new RecommendationContext(
                profile, preferred, excluded, saved, treatments, generatedAt);

        // Hard filters run before downloading catalog images. ONNX observations are cached in memory.
        var eligible = scorer.scoreAll(candidates, context).stream().map(ScoredRecommendation::candidate).toList();
        var similarities = photoResolver.similarities(photo.evidence(), eligible);
        var photoEvidence = photo.evidence().withComparedStyles(similarities.size());
        context = new RecommendationContext(profile, preferred, excluded, saved, treatments, generatedAt, similarities);
        String canonical = canonicalSnapshot(context, candidates, treatmentHistory) + "|photo:" + photoEvidence
                + "|similarities:" + new java.util.TreeMap<>(similarities);
        String inputHash = sha256(canonical);
        if (!forceRefresh) {
            RecommendationRun reusable = recommendationRepositoryPort.findActiveByInputHash(
                    userId, RecommendationRun.Strategy.RULE_BASED_V1.name(), inputHash).orElse(null);
            if (reusable != null && reusable.recommendationBasis() != null
                    && canReuseExplanation(reusable, generatedAt)) {
                return render(reusable);
            }
        }

        Map<UUID, Long> exposureCounts = forceRefresh
                ? recommendationRepositoryPort.findRecentExposureCounts(userId, inputHash) : Map.of();
        List<ScoredRecommendation> selected = diversifier.topThree(
                scorer.scoreAll(candidates, context), exposureCounts);
        if (selected.isEmpty()) {
            throw new RecommendationException(RecommendationError.NO_ELIGIBLE_CANDIDATES);
        }
        Map<UUID, TreatmentRecord> records = treatments.stream().collect(Collectors.toMap(
                TreatmentRecord::recordId, Function.identity()));
        UUID runId = UUID.randomUUID();
        Map<UUID, String> explanations = explanationPort.explain(selected, context);
        List<RecommendationItem> items = java.util.stream.IntStream.range(0, selected.size())
                .mapToObj(index -> toItem(selected.get(index), index + 1, records, explanations)).toList();
        RecommendationBasis basis = recommendationBasis(context, treatmentHistory, selected, photoEvidence);
        RecommendationRun run = new RecommendationRun(runId, userId,
                RecommendationRun.Strategy.RULE_BASED_V1, RecommendationRun.Status.ACTIVE,
                inputHash, context.coldStart(), basis, generatedAt, items);
        return render(recommendationRepositoryPort.insert(run, canonical));
    }

    private boolean canReuseExplanation(RecommendationRun run, Instant now) {
        if (!explanationPort.isEnabled()) return true;
        boolean complete = !run.items().isEmpty() && run.items().stream().allMatch(item ->
                item.reasons().stream().anyMatch(reason ->
                        !reason.params().getOrDefault("ai_message", "").isBlank()));
        // 실패 때 저장한 기본 설명도 잠시 재사용해 호출 폭주를 막고, 충전/장애 복구 후에는 재시도한다.
        return complete || now.isBefore(run.generatedAt().plus(Duration.ofMinutes(5)));
    }

    @Override
    @Transactional
    public RecommendationResult getLatest(UUID userId) {
        lockActiveAccount(userId);
        recommendationRepositoryPort.findLatestByUserId(userId)
                .orElseThrow(() -> new RecommendationException(RecommendationError.NOT_FOUND));
        // 화면 재방문 시에도 현재 입력/알고리즘으로 검증한다. 변경이 없으면 기존 실행을 재사용한다.
        return generateForLockedAccount(userId, false);
    }

    @Override
    public RecommendationResult get(UUID userId, UUID recommendationRunId) {
        return render(recommendationRepositoryPort.findByIdAndUserId(recommendationRunId, userId)
                .orElseThrow(() -> new RecommendationException(RecommendationError.NOT_FOUND)));
    }

    private RecommendationResult render(RecommendationRun run) {
        List<UUID> hairstyleIds = run.items().stream().map(RecommendationItem::hairstyleId).toList();
        Map<UUID, HairstyleCandidate> hairstyles = catalogRepositoryPort.findAllByIds(hairstyleIds)
                .stream().collect(Collectors.toMap(HairstyleCandidate::hairstyleId, Function.identity()));
        List<UUID> fileIds = hairstyles.values().stream().map(HairstyleCandidate::thumbnailFileId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<UUID, StoredFile> files = fileRepositoryPort.findAllById(fileIds).stream()
                .collect(Collectors.toMap(StoredFile::fileId, Function.identity()));
        Map<UUID, URI> urls = new LinkedHashMap<>();
        List<RecommendationResult.Item> items = run.items().stream().map(item -> {
            HairstyleCandidate hairstyle = hairstyles.get(item.hairstyleId());
            if (hairstyle == null) {
                throw new IllegalStateException("추천 스타일 카탈로그가 존재하지 않습니다: " + item.hairstyleId());
            }
            StoredFile file = files.get(hairstyle.thumbnailFileId());
            URI url = file == null || !file.isReady() ? null : urls.computeIfAbsent(
                    file.fileId(), ignored -> fileStoragePort.createDownloadUrl(file));
            return new RecommendationResult.Item(item, hairstyle, url);
        }).toList();
        return new RecommendationResult(run.recommendationRunId(), run.strategy(), run.status(),
                run.generatedAt(), run.fallback(), run.recommendationBasis(), items);
    }

    private RecommendationBasis recommendationBasis(
            RecommendationContext context,
            TreatmentHistorySummary treatmentHistory,
            List<ScoredRecommendation> selected,
            com.heddy.domain.recommendation.model.PhotoHairEvidence photoEvidence
    ) {
        HairProfile profile = context.hairProfile();
        RecommendationBasis.CurrentHair currentHair = profile == null ? null
                : new RecommendationBasis.CurrentHair(profile.hairType(), profile.hairCondition(),
                        profile.hairLength(), profile.hairThickness());
        return new RecommendationBasis(
                new RecommendationBasis.TreatmentHistory(
                        treatmentHistory.count(), treatmentHistory.highestSatisfaction()),
                Math.toIntExact(selected.stream()
                        .filter(result -> result.candidate().supportsAr()).count()),
                new RecommendationBasis.StylePreferences(
                        context.preferredTagIds().size(), context.excludedTagIds().size()),
                currentHair,
                profile == null ? null : profile.availableCareTimeMinutes(), photoEvidence);
    }

    private RecommendationItem toItem(
            ScoredRecommendation scored,
            int rank,
            Map<UUID, TreatmentRecord> records,
            Map<UUID, String> explanations
    ) {
        TreatmentRecord source = records.get(scored.referenceRecordId());
        RecommendationReference reference = source == null ? null : new RecommendationReference(
                source.recordId(), source.performedAt(), source.satisfaction(),
                scored.reasons().stream().filter(reason -> reason.params().containsKey("record_id"))
                        .map(reason -> reason.code().name()).findFirst().orElseThrow());
        return new RecommendationItem(UUID.randomUUID(), scored.candidate().hairstyleId(), null,
                rank, BigDecimal.valueOf(scored.finalScore()).setScale(2, RoundingMode.HALF_UP),
                scored.scoreBreakdown(), explainedReasons(scored, explanations), scored.candidate().managementDifficulty(),
                scored.candidate().estimatedDailyCareMinutes(), reference);
    }

    private List<RecommendationReason> explainedReasons(ScoredRecommendation scored,
                                                        Map<UUID, String> explanations) {
        String explanation = explanations.get(scored.candidate().hairstyleId());
        if (explanation == null || explanation.isBlank()) return scored.reasons();
        var reasons = new java.util.ArrayList<>(scored.reasons());
        if (reasons.isEmpty()) {
            reasons.add(new RecommendationReason(RecommendationReason.Code.EDITORIAL_FALLBACK, Map.of()));
        }
        RecommendationReason first = reasons.getFirst();
        Map<String, String> params = new LinkedHashMap<>(first.params());
        params.put("ai_message", explanation);
        params.put("explanation_version", explanationPort.cacheKey());
        reasons.set(0, new RecommendationReason(first.code(), params));
        return List.copyOf(reasons);
    }

    private Set<UUID> preferenceIds(
            Collection<UserStylePreference> preferences,
            UserStylePreference.PreferenceType type
    ) {
        return preferences.stream().filter(preference -> preference.preferenceType() == type)
                .map(UserStylePreference::styleTagId).collect(Collectors.toSet());
    }

    private String canonicalSnapshot(
            RecommendationContext context,
            List<HairstyleCandidate> candidates,
            TreatmentHistorySummary treatmentHistory
    ) {
        StringBuilder value = new StringBuilder("RULE_BASED_V1:PREFERENCE_PRIORITY_V7|");
        value.append(explanationPort.cacheKey()).append('|');
        HairProfile profile = context.hairProfile();
        if (profile != null) {
            value.append(profile.hairLength()).append('|').append(profile.hairType()).append('|')
                    .append(profile.hairThickness()).append('|').append(profile.hairCondition()).append('|')
                    .append(profile.availableCareTimeMinutes());
        }
        appendSorted(value, context.preferredTagIds());
        appendSorted(value, context.excludedTagIds());
        appendSorted(value, context.savedHairstyleIds());
        value.append('|').append(treatmentHistory.count()).append(':')
                .append(treatmentHistory.highestSatisfaction());
        context.recentTreatments().stream().sorted(Comparator.comparing(TreatmentRecord::recordId))
                .forEach(record -> value.append('|').append(record.recordId()).append(':')
                        .append(record.performedAt()).append(':').append(record.satisfaction()).append(':')
                        .append(record.serviceTypes().stream().map(Enum::name).sorted().toList())
                        .append(':').append(record.cutLength()).append(':').append(record.cutShape())
                        .append(':').append(record.permType()));
        candidates.stream().sorted(Comparator.comparing(HairstyleCandidate::hairstyleId))
                .forEach(candidate -> value.append('|').append(candidate.hairstyleId()).append(':')
                        .append(candidate.styleName()).append(':').append(candidate.assetVersion()).append(':').append(candidate.metadataVersion())
                        .append(':').append(candidate.arMode())
                        .append(':').append(candidate.arGroomName())
                        .append(':').append(candidate.category())
                        .append(':').append(candidate.thumbnailFileId())
                        .append(':').append(candidate.active())
                        .append(':').append(candidate.minimumHairLength())
                        .append(':').append(candidate.estimatedDailyCareMinutes())
                        .append(':').append(candidate.managementDifficulty())
                        .append(':').append(candidate.chemicalStressLevel())
                        .append(':').append(candidate.editorialPriority())
                        .append(':').append(candidate.serviceTypes().stream().sorted().toList())
                        .append(':').append(candidate.compatibleHairLengths().stream().sorted().toList())
                        .append(':').append(candidate.compatibleHairTypes().stream().sorted().toList())
                        .append(':').append(candidate.compatibleHairThicknesses().stream().sorted().toList())
                        .append(':').append(candidate.compatibleHairConditions().stream().sorted().toList())
                        .append(':').append(candidate.contraindicatedHairConditions().stream().sorted().toList())
                        .append(':').append(new java.util.TreeMap<>(candidate.tags())));
        return value.toString();
    }

    private void appendSorted(StringBuilder target, Collection<UUID> ids) {
        target.append('|').append(ids.stream().sorted().toList());
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", impossible);
        }
    }

    private void validateActiveAccount(Account account) {
        if (account.isDeleted()) {
            throw new AccountException(AccountError.ACCOUNT_DELETED);
        }
    }
}
