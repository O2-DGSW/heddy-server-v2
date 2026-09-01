package com.heddy.domain.recommendation.port.out;

import java.util.UUID;

@FunctionalInterface
public interface RecommendationStalenessPort {
    void markByReferenceRecordStale(UUID treatmentRecordId);
}
