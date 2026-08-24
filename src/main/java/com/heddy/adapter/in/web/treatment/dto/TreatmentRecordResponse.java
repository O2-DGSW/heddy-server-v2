package com.heddy.adapter.in.web.treatment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.model.TreatmentRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 시술기록 응답. 가격은 명세대로 객체 하나로 합쳐 내려간다(스키마에는 amount·currency 로 분리 저장).
 * photos 는 사진 식별자와 촬영 시점만 담는다 — URL 을 저장하지 않고 조회 때 발급한다.
 */
public record TreatmentRecordResponse(
        @Schema(description = "기록 식별자")
        @JsonProperty("record_id") UUID recordId,

        @Schema(description = "시술 종류 목록")
        @JsonProperty("service_types") Set<ServiceType> serviceTypes,

        @Schema(description = "미용실 이름. 입력하지 않았으면 비어 있다")
        @JsonProperty("salon_name") String salonName,

        @Schema(description = "디자이너 이름. 입력하지 않았으면 비어 있다")
        @JsonProperty("designer_name") String designerName,

        @Schema(description = "시술일시")
        @JsonProperty("performed_at") Instant performedAt,

        @Schema(description = "만족도(1~5). 입력하지 않았으면 비어 있다")
        @JsonProperty("satisfaction") Integer satisfaction,

        @Schema(description = "가격. 입력하지 않았으면 비어 있다")
        @JsonProperty("price") Price price,

        @Schema(description = "연결된 예약 식별자. 없으면 비어 있다")
        @JsonProperty("appointment_id") UUID appointmentId,

        @Schema(description = "기록 생성 시각")
        @JsonProperty("created_at") Instant createdAt,

        @Schema(description = "첨부 사진. 사진 식별자와 촬영 시점 구분만 담는다")
        @JsonProperty("photos") List<Photo> photos
) {

    public record Price(
            @Schema(description = "금액")
            @JsonProperty("amount") Long amount,

            @Schema(description = "통화 코드(3자)")
            @JsonProperty("currency") String currency
    ) {
    }

    public record Photo(
            @Schema(description = "사진 식별자")
            @JsonProperty("photo_id") UUID photoId,

            @Schema(description = "촬영 시점 구분. BEFORE AFTER OTHER")
            @JsonProperty("image_type") String imageType
    ) {
    }

    /** 등록 직후 응답. URL 이 필요 없는 만큼 사진은 식별자·촬영 시점만 담는다. */
    public static TreatmentRecordResponse core(TreatmentRecord record) {
        return of(record, record.photos().stream()
                .map(photo -> new Photo(photo.photoId(), photo.imageType().name()))
                .toList());
    }

    private static TreatmentRecordResponse of(TreatmentRecord record, List<Photo> photos) {
        return new TreatmentRecordResponse(
                record.recordId(), record.serviceTypes(), record.salonName(), record.designerName(),
                record.performedAt(), record.satisfaction(),
                record.priceAmount() == null ? null
                        : new Price(record.priceAmount(), record.priceCurrency()),
                record.appointmentId(), record.createdAt(), photos);
    }
}
