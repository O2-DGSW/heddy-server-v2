package com.heddy.adapter.out.persistence.recommendation;

import com.heddy.domain.recommendation.model.RecommendationReference;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class RecommendationReferenceQueryRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    Map<UUID, RecommendationReference> findByItemIds(Collection<UUID> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, RecommendationReference> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT reference.recommendation_item_id, reference.record_id,
                       reference.reference_reason_code, record.performed_at, record.satisfaction
                FROM recommendation_reference_records reference
                JOIN treatment_records record ON record.record_id = reference.record_id
                WHERE reference.recommendation_item_id IN (:ids)
                ORDER BY reference.recommendation_item_id, reference.record_id
                """, Map.of("ids", itemIds), (org.springframework.jdbc.core.RowCallbackHandler) rows -> result.put(
                rows.getObject("recommendation_item_id", UUID.class),
                new RecommendationReference(rows.getObject("record_id", UUID.class),
                        rows.getTimestamp("performed_at").toInstant(),
                        rows.getObject("satisfaction", Integer.class),
                        rows.getString("reference_reason_code"))));
        return result;
    }
}
