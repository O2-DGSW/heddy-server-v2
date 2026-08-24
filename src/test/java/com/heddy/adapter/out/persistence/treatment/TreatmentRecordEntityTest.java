package com.heddy.adapter.out.persistence.treatment;

import com.heddy.domain.treatment.exception.TreatmentError;
import com.heddy.domain.treatment.exception.TreatmentException;
import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.model.TreatmentPhoto;
import com.heddy.domain.treatment.model.TreatmentRecord;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TreatmentRecordEntityTest {

    private static final Instant PERFORMED_AT = Instant.parse("2026-08-01T10:00:00Z");

    @Test
    void carriesEveryDomainFieldThroughTheEntity() {
        UUID appointmentId = UUID.randomUUID();
        TreatmentRecord record = TreatmentRecord.create(
                UUID.randomUUID(), Set.of(ServiceType.CUT, ServiceType.COLOR), "준헤어", "김실장",
                PERFORMED_AT, 4, 120_000L, "KRW", appointmentId);

        TreatmentRecord roundTripped = new TreatmentRecordEntity(record).toDomain(List.of());

        assertThat(roundTripped.recordId()).isEqualTo(record.recordId());
        assertThat(roundTripped.userId()).isEqualTo(record.userId());
        assertThat(roundTripped.serviceTypes())
                .containsExactlyInAnyOrder(ServiceType.CUT, ServiceType.COLOR);
        assertThat(roundTripped.salonName()).isEqualTo("준헤어");
        assertThat(roundTripped.designerName()).isEqualTo("김실장");
        assertThat(roundTripped.performedAt()).isEqualTo(PERFORMED_AT);
        assertThat(roundTripped.satisfaction()).isEqualTo(4);
        assertThat(roundTripped.priceAmount()).isEqualTo(120_000L);
        assertThat(roundTripped.priceCurrency()).isEqualTo("KRW");
        assertThat(roundTripped.appointmentId()).isEqualTo(appointmentId);
        assertThat(roundTripped.photos()).isEmpty();
    }

    @Test
    void mapsSatisfactionNullThroughTheEntity() {
        TreatmentRecord record = TreatmentRecord.create(
                UUID.randomUUID(), Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                null, null, null, null);

        TreatmentRecord roundTripped = new TreatmentRecordEntity(record).toDomain(List.of());

        assertThat(roundTripped.satisfaction()).isNull();
        assertThat(roundTripped.priceAmount()).isNull();
        assertThat(roundTripped.priceCurrency()).isNull();
    }

    /**
     * 열거형 이름으로 저장하므로, 행에 남은 알 수 없는 이름(열거 축소 등)을 만나면
     * 직렬화 계층 예외가 아니라 도메인 오류로 막는다.
     */
    @Test
    void rejectsUnknownServiceTypeNameStoredInRow() {
        TreatmentRecord record = TreatmentRecord.create(
                UUID.randomUUID(), Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                null, null, null, null);
        TreatmentRecordEntity entity = new TreatmentRecordEntity(record);
        ReflectionTestUtils.setField(entity, "serviceTypes", Set.of("CUT", "WAVE"));

        assertThatThrownBy(() -> entity.toDomain(List.of()))
                .isInstanceOf(TreatmentException.class)
                .extracting(e -> ((TreatmentException) e).error())
                .isEqualTo(TreatmentError.SERVICE_TYPE_UNKNOWN);
    }

    @Test
    void photoEntityCarriesEveryFieldThroughTheEntity() {
        TreatmentPhoto photo = TreatmentPhoto.create(UUID.randomUUID(), UUID.randomUUID(), ImageType.BEFORE);

        TreatmentPhoto roundTripped = new TreatmentPhotoEntity(photo).toDomain();

        assertThat(roundTripped.photoId()).isEqualTo(photo.photoId());
        assertThat(roundTripped.recordId()).isEqualTo(photo.recordId());
        assertThat(roundTripped.fileId()).isEqualTo(photo.fileId());
        assertThat(roundTripped.imageType()).isEqualTo(ImageType.BEFORE);
        assertThat(roundTripped.createdAt()).isNull();
    }
}
