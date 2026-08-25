package com.heddy.adapter.in.web.treatment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.model.TreatmentRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 시술기록 응답. 가격은 명세대로 객체 하나로 합쳐 내려간다(스키마에는 amount·currency 로 분리 저장).
 * photos 의 URL 을 저장하지 않고 조회 때 발급한다 — 등록 응답엔 URL 이 아예 내려가지 않는다.
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

        @Schema(description = "개인 메모. 없으면 비어 있다")
        String memo,

        @Schema(description = "다음 방문 시 주의사항. 없으면 비어 있다")
        @JsonProperty("next_visit_cautions") String nextVisitCautions,

        @Schema(description = "기록 생성 시각")
        @JsonProperty("created_at") Instant createdAt,

        @Schema(description = "첨부 사진. 등록 응답은 식별자·촬영 시점만, 단건 조회는 photo_url 이 발급된다")
        @JsonProperty("photos") List<? extends Photo> photos
) {

    public record Price(
            @Schema(description = "금액")
            @JsonProperty("amount") Long amount,

            @Schema(description = "통화 코드(3자)")
            @JsonProperty("currency") String currency
    ) {
    }

    /**
     * 사진 응답의 공통 자리. 등록·조회가 photo_url 을 다르게 다뤄 타입을 나눴다 —
     * 한 타입에 NON_NULL 을 걸면 조회에서 URL 이 없을 때 필드가 통째로 사라져,
     * "READY 가 아닌 파일은 photo_url 을 null 로 내려준다"는 계약을 지킬 수 없다.
     */
    public sealed interface Photo permits CorePhoto, PhotoWithUrl {

        UUID photoId();

        String imageType();

        int sortOrder();
    }

    /** 등록 응답용. URL 을 발급하지 않으므로 photo_url 자리를 아예 두지 않는다. */
    @Schema(name = "TreatmentRecordCorePhoto")
    public record CorePhoto(
            @Schema(description = "사진 식별자")
            @JsonProperty("photo_id") UUID photoId,

            @Schema(description = "촬영 시점 구분. BEFORE AFTER OTHER")
            @JsonProperty("image_type") String imageType,

            @Schema(description = "표시 순서")
            @JsonProperty("sort_order") int sortOrder
    ) implements Photo {
    }

    /** 단건 조회 응답용. 파일이 READY 가 아니면 photo_url 은 null 로 남는다(필드는 유지된다). */
    @Schema(name = "TreatmentRecordPhotoWithUrl")
    public record PhotoWithUrl(
            @Schema(description = "사진 식별자")
            @JsonProperty("photo_id") UUID photoId,

            @Schema(description = "촬영 시점 구분. BEFORE AFTER OTHER")
            @JsonProperty("image_type") String imageType,

            @Schema(description = "표시 순서")
            @JsonProperty("sort_order") int sortOrder,

            @Schema(description = "Presigned GET URL. 짧은 만료로 조회 때마다 새로 발급되며 저장되지 "
                    + "않는다. READY 가 아닌 파일이면 null 이다", nullable = true)
            @JsonProperty("photo_url") String photoUrl
    ) implements Photo {
    }

    /** 등록 직후 응답. URL 이 필요 없는 만큼 사진은 식별자·촬영 시점만 담는다. */
    public static TreatmentRecordResponse core(TreatmentRecord record) {
        return of(record, record.photos().stream()
                .map(photo -> new CorePhoto(
                        photo.photoId(), photo.imageType().name(), photo.sortOrder()))
                .toList());
    }

    /** 단건 조회 응답. 사진마다 조회 시점에 발급한 URL 을 붙인다. */
    public static TreatmentRecordResponse withPhotos(TreatmentRecord record, Map<UUID, URI> photoUrls) {
        return of(record, record.photos().stream()
                .map(photo -> {
                    URI url = photoUrls.get(photo.photoId());
                    return new PhotoWithUrl(
                            photo.photoId(), photo.imageType().name(), photo.sortOrder(),
                            url == null ? null : url.toString());
                })
                .toList());
    }

    private static TreatmentRecordResponse of(TreatmentRecord record, List<? extends Photo> photos) {
        return new TreatmentRecordResponse(
                record.recordId(), record.serviceTypes(), record.salonName(), record.designerName(),
                record.performedAt(), record.satisfaction(),
                record.priceAmount() == null ? null
                        : new Price(record.priceAmount(), record.priceCurrency()),
                record.appointmentId(), record.memo(), record.nextVisitCautions(),
                record.createdAt(), photos);
    }
}
