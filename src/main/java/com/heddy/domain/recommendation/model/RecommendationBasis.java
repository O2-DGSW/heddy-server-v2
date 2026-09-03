package com.heddy.domain.recommendation.model;

import com.heddy.domain.account.model.HairProfile.HairCondition;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.account.model.HairProfile.HairThickness;
import com.heddy.domain.account.model.HairProfile.HairType;

/** 추천 생성 시점에 사용한 사용자 입력과 결과 특성을 설명하는 스냅샷. */
public record RecommendationBasis(
        TreatmentHistory treatmentHistory,
        int arCandidateStyleCount,
        StylePreferences stylePreferences,
        CurrentHair currentHair,
        Integer availableCareTimeMinutes
) {
    public record TreatmentHistory(long count, Integer highestSatisfaction) { }

    public record StylePreferences(int preferredCount, int excludedCount) { }

    public record CurrentHair(
            HairType hairType,
            HairCondition hairCondition,
            HairLength hairLength,
            HairThickness hairThickness
    ) { }
}
