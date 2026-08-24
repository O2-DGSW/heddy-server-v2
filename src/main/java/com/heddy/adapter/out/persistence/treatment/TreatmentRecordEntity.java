package com.heddy.adapter.out.persistence.treatment;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.model.TreatmentPhoto;
import com.heddy.domain.treatment.model.TreatmentRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 시술기록의 JPA 표현. {@code service_types} 는 JSONB 컬럼으로, Hibernate 의 JSON 매핑이
 * 집합을 문자열 배열로 직렬화한다. 순서는 의미가 없으므로 읽을 때 LinkedHashSet 으로만
 * 안정성을 챙긴다.
 */
@Entity
@Table(name = "treatment_records")
class TreatmentRecordEntity extends BaseEntity {

    @Id
    @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "service_types", nullable = false, updatable = false)
    private Set<ServiceType> serviceTypes = new LinkedHashSet<>();

    @Column(name = "salon_name", length = 50, updatable = false)
    private String salonName;

    @Column(name = "designer_name", length = 30, updatable = false)
    private String designerName;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt;

    @Column(name = "satisfaction")
    private Short satisfaction;

    @Column(name = "price_amount")
    private Long priceAmount;

    @Column(name = "price_currency", length = 3)
    private String priceCurrency;

    @Column(name = "appointment_id", updatable = false)
    private UUID appointmentId;

    protected TreatmentRecordEntity() {
    }

    TreatmentRecordEntity(TreatmentRecord record) {
        recordId = record.recordId();
        userId = record.userId();
        serviceTypes = new LinkedHashSet<>(record.serviceTypes());
        salonName = record.salonName();
        designerName = record.designerName();
        performedAt = record.performedAt();
        satisfaction = record.satisfaction() == null ? null : record.satisfaction().shortValue();
        priceAmount = record.priceAmount();
        priceCurrency = record.priceCurrency();
        appointmentId = record.appointmentId();
    }

    TreatmentRecord toDomain(List<TreatmentPhoto> photos) {
        return new TreatmentRecord(
                recordId, userId, serviceTypes, salonName, designerName, performedAt,
                satisfaction == null ? null : satisfaction.intValue(),
                priceAmount, priceCurrency, appointmentId,
                photos, getCreatedAt());
    }
}
