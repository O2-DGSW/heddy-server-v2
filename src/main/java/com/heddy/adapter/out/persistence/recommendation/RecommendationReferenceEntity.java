package com.heddy.adapter.out.persistence.recommendation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "recommendation_reference_records")
@IdClass(RecommendationReferenceEntity.Key.class)
class RecommendationReferenceEntity {
    @Id @Column(name = "recommendation_item_id", nullable = false, updatable = false)
    private UUID recommendationItemId;
    @Id @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;
    @Column(name = "reference_reason", nullable = false, length = 255, updatable = false)
    private String referenceReasonCode;

    @Column(name = "performed_at_snapshot", nullable = false, updatable = false)
    private java.time.Instant performedAtSnapshot;
    @Column(name = "satisfaction_snapshot", updatable = false)
    private Integer satisfactionSnapshot;

    protected RecommendationReferenceEntity() { }

    RecommendationReferenceEntity(UUID recommendationItemId, com.heddy.domain.recommendation.model.RecommendationReference reference) {
        this.recommendationItemId = recommendationItemId;
        this.recordId = reference.recordId();
        this.referenceReasonCode = reference.reasonCode();
        this.performedAtSnapshot = reference.performedAt();
        this.satisfactionSnapshot = reference.satisfaction();
    }

    static class Key implements Serializable {
        private UUID recommendationItemId;
        private UUID recordId;
        public Key() { }
        @Override public boolean equals(Object other) {
            return other instanceof Key key && Objects.equals(recommendationItemId, key.recommendationItemId)
                    && Objects.equals(recordId, key.recordId);
        }
        @Override public int hashCode() { return Objects.hash(recommendationItemId, recordId); }
    }
}
