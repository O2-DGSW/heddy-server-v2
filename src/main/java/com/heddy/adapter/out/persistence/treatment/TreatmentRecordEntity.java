package com.heddy.adapter.out.persistence.treatment;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.treatment.exception.TreatmentError;
import com.heddy.domain.treatment.exception.TreatmentException;
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
import java.util.stream.Collectors;

/**
 * 시술기록의 JPA 표현. {@code service_types} 는 JSONB 컬럼으로 문자열 배열을 저장한다.
 * 열거형을 곧바로 묶지 않고 이름으로만 저장하는 이유는, 행에서 알 수 없는 이름이 읽힐 때
 * 직렬화 계층의 예외 대신 도메인 오류로 막기 위해서다.
 *
 * <p>사용자가 고치는 비즈니스 필드(PATCH /treatment-records/{recordId})는 {@code updatable}
 * 제약을 두지 않는다. 제약을 걸어두면 병합 시 조용히 누락돼 수정이 사라진다.
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
    @Column(name = "service_types", nullable = false)
    private Set<String> serviceTypes = new LinkedHashSet<>();

    @Column(name = "salon_name", length = 50)
    private String salonName;

    @Column(name = "designer_name", length = 30)
    private String designerName;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "satisfaction")
    private Short satisfaction;

    @Column(name = "price_amount")
    private Long priceAmount;

    @Column(name = "price_currency", length = 3)
    private String priceCurrency;

    private UUID appointmentId;

    @Column(columnDefinition = "text")
    private String memo;

    @Column(name = "next_visit_cautions", columnDefinition = "text")
    private String nextVisitCautions;

    @Column(name = "duration_minutes")
    private Short durationMinutes;

    @Column(name = "treatment_content", length = 255)
    private String treatmentContent;

    protected TreatmentRecordEntity() {
    }

    TreatmentRecordEntity(TreatmentRecord record) {
        recordId = record.recordId();
        userId = record.userId();
        serviceTypes = record.serviceTypes().stream()
                .map(ServiceType::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        salonName = record.salonName();
        designerName = record.designerName();
        performedAt = record.performedAt();
        satisfaction = record.satisfaction() == null ? null : record.satisfaction().shortValue();
        priceAmount = record.priceAmount();
        priceCurrency = record.priceCurrency();
        appointmentId = record.appointmentId();
        memo = record.memo();
        nextVisitCautions = record.nextVisitCautions();
        durationMinutes = record.durationMinutes() == null
                ? null : record.durationMinutes().shortValue();
        treatmentContent = record.treatmentContent();
    }

    TreatmentRecord toDomain(List<TreatmentPhoto> photos) {
        Set<ServiceType> parsedServiceTypes = serviceTypes.stream()
                .map(TreatmentRecordEntity::parseServiceType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new TreatmentRecord(
                recordId, userId, parsedServiceTypes, salonName, designerName, performedAt,
                satisfaction == null ? null : satisfaction.intValue(),
                priceAmount, priceCurrency, appointmentId, memo, nextVisitCautions,
                durationMinutes == null ? null : durationMinutes.intValue(), treatmentContent,
                photos, getCreatedAt());
    }

    UUID recordId() {
        return recordId;
    }

    void update(TreatmentRecord record) {
        serviceTypes = record.serviceTypes().stream()
                .map(ServiceType::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        salonName = record.salonName();
        designerName = record.designerName();
        performedAt = record.performedAt();
        satisfaction = record.satisfaction() == null ? null : record.satisfaction().shortValue();
        priceAmount = record.priceAmount();
        priceCurrency = record.priceCurrency();
        appointmentId = record.appointmentId();
        memo = record.memo();
        nextVisitCautions = record.nextVisitCautions();
        durationMinutes = record.durationMinutes() == null
                ? null : record.durationMinutes().shortValue();
        treatmentContent = record.treatmentContent();
    }

    private static ServiceType parseServiceType(String name) {
        try {
            return ServiceType.valueOf(name);
        } catch (IllegalArgumentException invalidName) {
            throw new TreatmentException(TreatmentError.SERVICE_TYPE_UNKNOWN);
        }
    }
}
