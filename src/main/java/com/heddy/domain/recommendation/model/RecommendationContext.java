package com.heddy.domain.recommendation.model;

import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.treatment.model.TreatmentRecord;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record RecommendationContext(
        HairProfile hairProfile,
        Set<UUID> preferredTagIds,
        Set<UUID> excludedTagIds,
        Set<UUID> savedHairstyleIds,
        List<TreatmentRecord> recentTreatments,
        Instant generatedAt
) {
    public RecommendationContext {
        preferredTagIds = preferredTagIds == null ? Set.of() : Set.copyOf(preferredTagIds);
        excludedTagIds = excludedTagIds == null ? Set.of() : Set.copyOf(excludedTagIds);
        savedHairstyleIds = savedHairstyleIds == null ? Set.of() : Set.copyOf(savedHairstyleIds);
        recentTreatments = recentTreatments == null ? List.of() : List.copyOf(recentTreatments);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }

    public boolean coldStart() {
        boolean hasProfile = hairProfile != null && (hairProfile.hairLength() != null
                || hairProfile.hairType() != null || hairProfile.hairThickness() != null
                || hairProfile.hairCondition() != null || hairProfile.availableCareTimeMinutes() != null);
        boolean hasHistory = recentTreatments.stream().anyMatch(record -> record.satisfaction() != null);
        return !hasProfile && !hasHistory && preferredTagIds.isEmpty() && savedHairstyleIds.isEmpty();
    }
}
