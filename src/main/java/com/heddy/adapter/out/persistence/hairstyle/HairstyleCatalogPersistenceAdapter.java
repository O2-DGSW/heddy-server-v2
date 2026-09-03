package com.heddy.adapter.out.persistence.hairstyle;

import com.heddy.domain.account.model.HairProfile.HairCondition;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.account.model.HairProfile.HairThickness;
import com.heddy.domain.account.model.HairProfile.HairType;
import com.heddy.domain.recommendation.model.HairstyleCandidate;
import com.heddy.domain.recommendation.model.HairstyleCandidate.ChemicalStressLevel;
import com.heddy.domain.recommendation.model.HairstyleCandidate.ManagementDifficulty;
import com.heddy.domain.recommendation.port.out.HairstyleCatalogRepositoryPort;
import com.heddy.domain.treatment.model.ServiceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HairstyleCatalogPersistenceAdapter implements HairstyleCatalogRepositoryPort {
    private final HairstyleAssetJpaRepository assetRepository;
    private final HairstyleRecommendationProfileJpaRepository profileRepository;
    private final HairstyleTagQueryRepository tagRepository;

    @Override
    public List<HairstyleCandidate> findEligibleCandidates() {
        return assemble(assetRepository.findEligible());
    }

    @Override
    public List<HairstyleCandidate> findAllByIds(Collection<UUID> hairstyleIds) {
        if (hairstyleIds.isEmpty()) {
            return List.of();
        }
        return assemble(assetRepository.findAllById(hairstyleIds));
    }

    private List<HairstyleCandidate> assemble(List<HairstyleAssetEntity> assets) {
        if (assets.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = assets.stream().map(HairstyleAssetEntity::hairstyleId).toList();
        Map<UUID, HairstyleRecommendationProfileEntity> profiles = profileRepository.findAllById(ids)
                .stream().collect(Collectors.toMap(
                        HairstyleRecommendationProfileEntity::hairstyleId, Function.identity()));
        Map<UUID, Map<UUID, String>> tags = new LinkedHashMap<>();
        tagRepository.findByHairstyleIds(ids).forEach(row -> tags
                .computeIfAbsent(row.hairstyleId(), ignored -> new LinkedHashMap<>())
                .put(row.styleTagId(), row.tagName()));
        return assets.stream()
                .filter(asset -> profiles.containsKey(asset.hairstyleId()))
                .map(asset -> toDomain(asset, profiles.get(asset.hairstyleId()),
                        tags.getOrDefault(asset.hairstyleId(), Map.of())))
                .toList();
    }

    private HairstyleCandidate toDomain(
            HairstyleAssetEntity asset,
            HairstyleRecommendationProfileEntity profile,
            Map<UUID, String> tags
    ) {
        return new HairstyleCandidate(asset.hairstyleId(), asset.styleName(), asset.category(),
                asset.thumbnailFileId(), asset.arMode(), asset.active(), asset.assetVersion(),
                parse(profile.serviceTypes(), ServiceType.class),
                parse(profile.compatibleHairLengths(), HairLength.class),
                parse(profile.compatibleHairTypes(), HairType.class),
                parse(profile.compatibleHairThicknesses(), HairThickness.class),
                parse(profile.compatibleHairConditions(), HairCondition.class),
                parse(profile.contraindicatedHairConditions(), HairCondition.class),
                parseNullable(profile.minimumHairLength(), HairLength.class),
                profile.estimatedDailyCareMinutes(),
                ManagementDifficulty.valueOf(profile.managementDifficulty()),
                ChemicalStressLevel.valueOf(profile.chemicalStressLevel()),
                profile.editorialPriority(), profile.metadataVersion(), tags);
    }

    private static <E extends Enum<E>> Set<E> parse(Set<String> values, Class<E> type) {
        return values.stream().map(value -> Enum.valueOf(type, value)).collect(Collectors.toSet());
    }

    private static <E extends Enum<E>> E parseNullable(String value, Class<E> type) {
        return value == null ? null : Enum.valueOf(type, value);
    }
}
