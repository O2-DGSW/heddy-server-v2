package com.heddy.domain.recommendation.model;

import java.time.Instant;
import java.util.UUID;

public record RecommendationReference(
        UUID recordId,
        Instant performedAt,
        Integer satisfaction,
        String reasonCode
) {
}
