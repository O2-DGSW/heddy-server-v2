package com.heddy.adapter.out.persistence.recommendation;

import com.heddy.domain.recommendation.model.RecommendationItem;
import com.heddy.domain.recommendation.model.RecommendationReference;
import com.heddy.domain.recommendation.model.RecommendationRun;
import com.heddy.domain.recommendation.port.out.RecommendationRepositoryPort;
import com.heddy.domain.recommendation.port.out.RecommendationStalenessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecommendationPersistenceAdapter
        implements RecommendationRepositoryPort, RecommendationStalenessPort {
    private final RecommendationRunJpaRepository runRepository;
    private final RecommendationItemJpaRepository itemRepository;
    private final RecommendationReferenceJpaRepository referenceRepository;
    private final RecommendationReferenceQueryRepository referenceQueryRepository;

    @Override
    public RecommendationRun insert(RecommendationRun run, String canonicalInputSnapshot) {
        runRepository.saveAndFlush(new RecommendationRunEntity(run, canonicalInputSnapshot));
        itemRepository.saveAllAndFlush(run.items().stream()
                .map(item -> new RecommendationItemEntity(run.recommendationRunId(), item)).toList());
        List<RecommendationReferenceEntity> references = run.items().stream()
                .filter(item -> item.referenceRecord() != null)
                .map(item -> new RecommendationReferenceEntity(item.recommendationItemId(),
                        item.referenceRecord().recordId(), item.referenceRecord().reasonCode()))
                .toList();
        if (!references.isEmpty()) {
            referenceRepository.saveAllAndFlush(references);
        }
        return run;
    }

    @Override
    public Optional<RecommendationRun> findActiveByInputHash(
            UUID userId, String strategy, String inputHash
    ) {
        return runRepository
                .findFirstByUserIdAndStrategyAndInputHashAndStatusOrderByGeneratedAtDescRecommendationRunIdDesc(
                        userId, strategy, inputHash, RecommendationRun.Status.ACTIVE.name())
                .map(this::assemble);
    }

    @Override
    public Optional<RecommendationRun> findLatestByUserId(UUID userId) {
        return runRepository.findFirstByUserIdOrderByGeneratedAtDescRecommendationRunIdDesc(userId)
                .map(this::assemble);
    }

    @Override
    public Optional<RecommendationRun> findByIdAndUserId(UUID recommendationRunId, UUID userId) {
        return runRepository.findByRecommendationRunIdAndUserId(recommendationRunId, userId)
                .map(this::assemble);
    }

    @Override
    public void markByReferenceRecordStale(UUID treatmentRecordId) {
        runRepository.markStaleByReferenceRecordId(treatmentRecordId);
    }

    private RecommendationRun assemble(RecommendationRunEntity run) {
        List<RecommendationItemEntity> entities = itemRepository
                .findByRecommendationRunIdOrderByDisplayRankAsc(run.recommendationRunId());
        Map<UUID, RecommendationReference> references = referenceQueryRepository.findByItemIds(
                entities.stream().map(RecommendationItemEntity::recommendationItemId).toList());
        List<RecommendationItem> items = entities.stream().map(entity -> entity.toDomain(
                references.get(entity.recommendationItemId()))).toList();
        return run.toDomain(items);
    }
}
