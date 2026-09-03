package com.heddy.domain.recommendation.port.in;

import java.util.UUID;

public interface GenerateRecommendationUseCase {
    RecommendationResult generate(UUID userId, boolean forceRefresh);
}
