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
import com.heddy.domain.recommendation.service.RecommendationDiversifier;
import com.heddy.domain.recommendation.service.RuleBasedV1Scorer;
import com.heddy.domain.style.model.UserStylePreference;
import com.heddy.domain.style.port.out.SavedStyleRepositoryPort;
import com.heddy.domain.style.port.out.UserStylePreferenceRepositoryPort;
import com.heddy.domain.treatment.model.TreatmentRecord;
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
    private final RuleBasedV1Scorer scorer;
    private final RecommendationDiversifier diversifier;
    private final Clock clock;

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
            FileStoragePort fileStoragePort
    ) {
        this(accountRepositoryPort, hairProfileRepositoryPort, preferenceRepositoryPort,
                savedStyleRepositoryPort, treatmentRepositoryPort, catalogRepositoryPort,
                recommendationRepositoryPort, fileRepositoryPort, fileStoragePort,
                new RuleBasedV1Scorer(), new RecommendationDiversifier(), Clock.systemUTC());
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
            RuleBasedV1Scorer scorer,
            RecommendationDiversifier diversifier,
            Clock clock
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
        this.scorer = scorer;
        this.diversifier = diversifier;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RecommendationResult generate(UUID userId, boolean forceRefresh) {
        validateActiveAccount(accountRepositoryPort.findByIdForUpdate(userId)
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND)));
        Instant generatedAt = clock.instant();
        HairProfile profile = hairProfileRepositoryPort.findByUserId(userId).orElse(null);
        List<UserStylePreference> preferences = preferenceRepositoryPort.findAllByUserId(userId);
        Set<UUID> preferred = preferenceIds(preferences, UserStylePreference.PreferenceType.PREFERRED);
        Set<UUID> excluded = preferenceIds(preferences, UserStylePreference.PreferenceType.EXCLUDED);
        Set<UUID> saved = Set.copyOf(savedStyleRepositoryPort.findHairstyleIdsByUserId(userId));
        List<TreatmentRecord> treatments = treatmentRepositoryPort.findRecentByUserId(userId, 10);
        List<HairstyleCandidate> candidates = catalogRepositoryPort.findEligibleCandidates();
        RecommendationContext context = new RecommendationContext(
                profile, preferred, excluded, saved, treatments, generatedAt);

        String canonical = canonicalSnapshot(context, candidates);
        String inputHash = sha256(canonical);
        if (!forceRefresh) {
            RecommendationRun reusable = recommendationRepositoryPort.findActiveByInputHash(
                    userId, RecommendationRun.Strategy.RULE_BASED_V1.name(), inputHash).orElse(null);
            if (reusable != null) {
                return render(reusable);
            }
        }

        List<ScoredRecommendation> selected = diversifier.topThree(
                scorer.scoreAll(candidates, context));
        if (selected.isEmpty()) {
            throw new RecommendationException(RecommendationError.NO_ELIGIBLE_CANDIDATES);
        }
        Map<UUID, TreatmentRecord> records = treatments.stream().collect(Collectors.toMap(
                TreatmentRecord::recordId, Function.identity()));
        UUID runId = UUID.randomUUID();
        List<RecommendationItem> items = java.util.stream.IntStream.range(0, selected.size())
                .mapToObj(index -> toItem(selected.get(index), index + 1, records)).toList();
        RecommendationRun run = new RecommendationRun(runId, userId,
                RecommendationRun.Strategy.RULE_BASED_V1, RecommendationRun.Status.ACTIVE,
                inputHash, context.coldStart(), generatedAt, items);
        return render(recommendationRepositoryPort.insert(run, canonical));
    }

    @Override
    public RecommendationResult getLatest(UUID userId) {
        return render(recommendationRepositoryPort.findLatestByUserId(userId)
                .orElseThrow(() -> new RecommendationException(RecommendationError.NOT_FOUND)));
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
                run.generatedAt(), run.fallback(), items);
    }

    private RecommendationItem toItem(
            ScoredRecommendation scored,
            int rank,
            Map<UUID, TreatmentRecord> records
    ) {
        TreatmentRecord source = records.get(scored.referenceRecordId());
        RecommendationReference reference = source == null ? null : new RecommendationReference(
                source.recordId(), source.performedAt(), source.satisfaction(),
                RecommendationReason.Code.SIMILAR_HIGH_SATISFACTION_HISTORY.name());
        return new RecommendationItem(UUID.randomUUID(), scored.candidate().hairstyleId(), null,
                rank, BigDecimal.valueOf(scored.finalScore()).setScale(2, RoundingMode.HALF_UP),
                scored.scoreBreakdown(), scored.reasons(), scored.candidate().managementDifficulty(),
                scored.candidate().estimatedDailyCareMinutes(), reference);
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
            List<HairstyleCandidate> candidates
    ) {
        StringBuilder value = new StringBuilder("RULE_BASED_V1|");
        HairProfile profile = context.hairProfile();
        if (profile != null) {
            value.append(profile.hairLength()).append('|').append(profile.hairType()).append('|')
                    .append(profile.hairThickness()).append('|').append(profile.hairCondition()).append('|')
                    .append(profile.availableCareTimeMinutes());
        }
        appendSorted(value, context.preferredTagIds());
        appendSorted(value, context.excludedTagIds());
        appendSorted(value, context.savedHairstyleIds());
        context.recentTreatments().stream().sorted(Comparator.comparing(TreatmentRecord::recordId))
                .forEach(record -> value.append('|').append(record.recordId()).append(':')
                        .append(record.performedAt()).append(':').append(record.satisfaction()).append(':')
                        .append(record.serviceTypes().stream().map(Enum::name).sorted().toList()));
        candidates.stream().sorted(Comparator.comparing(HairstyleCandidate::hairstyleId))
                .forEach(candidate -> value.append('|').append(candidate.hairstyleId()).append(':')
                        .append(candidate.assetVersion()).append(':').append(candidate.metadataVersion()));
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
