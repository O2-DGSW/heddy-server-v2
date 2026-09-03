package com.heddy.domain.recommendation.model;

import com.heddy.domain.account.model.HairProfile.HairCondition;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.account.model.HairProfile.HairThickness;
import com.heddy.domain.account.model.HairProfile.HairType;
import com.heddy.domain.treatment.model.ServiceType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** 추천 계산에 필요한 스타일 카탈로그 데이터를 한 번에 적재한 읽기 모델. */
public record HairstyleCandidate(
        UUID hairstyleId,
        String styleName,
        String category,
        UUID thumbnailFileId,
        String arMode,
        boolean active,
        String assetVersion,
        Set<ServiceType> serviceTypes,
        Set<HairLength> compatibleHairLengths,
        Set<HairType> compatibleHairTypes,
        Set<HairThickness> compatibleHairThicknesses,
        Set<HairCondition> compatibleHairConditions,
        Set<HairCondition> contraindicatedHairConditions,
        HairLength minimumHairLength,
        int estimatedDailyCareMinutes,
        ManagementDifficulty managementDifficulty,
        ChemicalStressLevel chemicalStressLevel,
        int editorialPriority,
        String metadataVersion,
        Map<UUID, String> tags
) {
    public enum ManagementDifficulty { LOW, MEDIUM, HIGH }
    public enum ChemicalStressLevel { LOW, MEDIUM, HIGH }

    public HairstyleCandidate {
        Objects.requireNonNull(hairstyleId, "hairstyleId");
        Objects.requireNonNull(styleName, "styleName");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(arMode, "arMode");
        Objects.requireNonNull(assetVersion, "assetVersion");
        Objects.requireNonNull(managementDifficulty, "managementDifficulty");
        Objects.requireNonNull(chemicalStressLevel, "chemicalStressLevel");
        Objects.requireNonNull(metadataVersion, "metadataVersion");
        serviceTypes = immutable(serviceTypes);
        compatibleHairLengths = immutable(compatibleHairLengths);
        compatibleHairTypes = immutable(compatibleHairTypes);
        compatibleHairThicknesses = immutable(compatibleHairThicknesses);
        compatibleHairConditions = immutable(compatibleHairConditions);
        contraindicatedHairConditions = immutable(contraindicatedHairConditions);
        tags = tags == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(tags));
        if (estimatedDailyCareMinutes < 0) {
            throw new IllegalArgumentException("estimatedDailyCareMinutes는 음수일 수 없습니다.");
        }
    }

    private static <T> Set<T> immutable(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    public UUID representativeTagId() {
        return tags.keySet().stream().sorted().findFirst().orElse(null);
    }

    public boolean supportsAr() {
        return !"NONE".equals(arMode);
    }
}
