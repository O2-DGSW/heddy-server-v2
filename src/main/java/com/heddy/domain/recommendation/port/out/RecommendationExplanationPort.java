package com.heddy.domain.recommendation.port.out;

import com.heddy.domain.recommendation.model.RecommendationContext;
import com.heddy.domain.recommendation.model.ScoredRecommendation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 이미 선택된 후보의 설명만 생성한다. 실패하면 빈 맵을 반환한다. */
public interface RecommendationExplanationPort {
    Map<UUID, String> explain(List<ScoredRecommendation> selected, RecommendationContext context);

    /** 설명 모델/프롬프트가 바뀌면 이전 추천 캐시를 재사용하지 않는다. */
    String cacheKey();
}
