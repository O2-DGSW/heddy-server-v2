package com.heddy.domain.treatment.model;

import com.heddy.domain.treatment.exception.TreatmentError;
import com.heddy.domain.treatment.exception.TreatmentException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 시술기록 한 건과 그에 첨부된 사진들. 시술 도메인의 애그리게이트 루트다.
 *
 * <p>불변식은 이 모델만 책임진다. API 계층이 같은 규칙을 다시 검사하면 두 곳 중 하나는
 * 반드시 뒤처지므로, 검증은 여기서 끝낸다(#31 이 만들 컨트롤러도 예외가 아니다).
 *
 * <ul>
 *   <li>{@code performedAt} 은 미래일 수 없다 — 아직 겪지 않은 시술을 기록할 수 없다</li>
 *   <li>{@code serviceTypes} 는 1개 이상 — 시술 없는 기록은 성립하지 않는다</li>
 *   <li>사진은 기록당 최대 {@value #MAX_PHOTOS}장 — 상세 화면·분석 입력의 상한이다</li>
 *   <li>만족도는 비어 있거나 1~5 — 선택 입력이지만 범위를 벗어날 수는 없다</li>
 *   <li>가격은 금액·통화가 한 쌍 — 응답에서 price 객체 하나로 합쳐지는 저장 형태다</li>
 * </ul>
 *
 * <p>살롱·디자이너는 엔티티가 아니라 문자열 필드다(v2 축소 스코프). 길이 상한은 테이블
 * 컬럼 크기와 맞춰져 있으므로, 이 모델을 통과한 값은 영속성 계층에서 잘리지 않는다.
 */
public record TreatmentRecord(
        UUID recordId,
        UUID userId,
        Set<ServiceType> serviceTypes,
        String salonName,
        String designerName,
        Instant performedAt,
        Integer satisfaction,
        Long priceAmount,
        String priceCurrency,
        UUID appointmentId,
        String memo,
        String nextVisitCautions,
        List<TreatmentPhoto> photos,
        Instant createdAt
) {
    public static final int MAX_PHOTOS = 10;
    private static final int SALON_NAME_MAX_LENGTH = 50;
    private static final int DESIGNER_NAME_MAX_LENGTH = 30;

    public TreatmentRecord {
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(userId, "userId");
        if (performedAt == null) {
            throw new TreatmentException(TreatmentError.PERFORMED_AT_REQUIRED);
        }

        if (serviceTypes == null || serviceTypes.isEmpty()) {
            throw new TreatmentException(TreatmentError.SERVICE_TYPE_REQUIRED);
        }
        serviceTypes = Set.copyOf(serviceTypes);

        salonName = normalizeName(salonName, SALON_NAME_MAX_LENGTH, TreatmentError.SALON_NAME_TOO_LONG);
        designerName = normalizeName(designerName, DESIGNER_NAME_MAX_LENGTH, TreatmentError.DESIGNER_NAME_TOO_LONG);

        if (satisfaction != null && (satisfaction < 1 || satisfaction > 5)) {
            throw new TreatmentException(TreatmentError.SATISFACTION_OUT_OF_RANGE);
        }

        if ((priceAmount == null) != (priceCurrency == null)) {
            throw new TreatmentException(TreatmentError.PRICE_INCOMPLETE);
        }
        if (priceAmount != null) {
            if (priceAmount < 0) {
                throw new TreatmentException(TreatmentError.PRICE_AMOUNT_NEGATIVE);
            }
            priceCurrency = normalizeCurrency(priceCurrency);
        }

        if (performedAt.isAfter(Instant.now())) {
            throw new TreatmentException(TreatmentError.PERFORMED_AT_IN_FUTURE);
        }

        memo = normalizeText(memo);
        nextVisitCautions = normalizeText(nextVisitCautions);

        photos = photos == null ? List.of() : List.copyOf(photos);
        if (photos.size() > MAX_PHOTOS) {
            throw new TreatmentException(TreatmentError.PHOTO_LIMIT_EXCEEDED);
        }
        photos.forEach(photo -> {
            if (!photo.recordId().equals(recordId)) {
                throw new TreatmentException(TreatmentError.PHOTO_RECORD_MISMATCH);
            }
        });
    }

    /** 메모 컬럼 도입 전 호출부와의 호환을 위한 생성자. */
    public TreatmentRecord(
            UUID recordId,
            UUID userId,
            Set<ServiceType> serviceTypes,
            String salonName,
            String designerName,
            Instant performedAt,
            Integer satisfaction,
            Long priceAmount,
            String priceCurrency,
            UUID appointmentId,
            List<TreatmentPhoto> photos,
            Instant createdAt
    ) {
        this(recordId, userId, serviceTypes, salonName, designerName, performedAt,
                satisfaction, priceAmount, priceCurrency, appointmentId,
                null, null, photos, createdAt);
    }

    /** 새 기록을 만든다. 식별자는 도메인이 발급하고 사진은 빈 채로 시작한다. */
    public static TreatmentRecord create(
            UUID userId,
            Set<ServiceType> serviceTypes,
            String salonName,
            String designerName,
            Instant performedAt,
            Integer satisfaction,
            Long priceAmount,
            String priceCurrency,
            UUID appointmentId
    ) {
        return create(userId, serviceTypes, salonName, designerName, performedAt,
                satisfaction, priceAmount, priceCurrency, appointmentId, null, null);
    }

    /** 메모와 다음 방문 주의사항을 포함해 새 기록을 만든다. */
    public static TreatmentRecord create(
            UUID userId,
            Set<ServiceType> serviceTypes,
            String salonName,
            String designerName,
            Instant performedAt,
            Integer satisfaction,
            Long priceAmount,
            String priceCurrency,
            UUID appointmentId,
            String memo,
            String nextVisitCautions
    ) {
        return new TreatmentRecord(
                UUID.randomUUID(), userId, serviceTypes, salonName, designerName,
                performedAt, satisfaction, priceAmount, priceCurrency, appointmentId,
                memo, nextVisitCautions, List.of(), null);
    }

    /**
     * 사진을 붙인 새 기록을 반환한다. 최대 장수를 채웠으면 거부한다.
     *
     * <p>이 검사는 모델이 들고 있는 사진 목록 기준이다. 두 요청이 동시에 같은 기록을 고르면
     * 저장 계층에서 둘 다 통과할 수 있으니, 유스케이스(#31)가 이 경합을 잠그는 책임을 진다.
     */
    public TreatmentRecord attachPhoto(TreatmentPhoto photo) {
        Objects.requireNonNull(photo, "photo");
        if (!photo.recordId().equals(recordId)) {
            throw new TreatmentException(TreatmentError.PHOTO_RECORD_MISMATCH);
        }
        if (photos.size() >= MAX_PHOTOS) {
            throw new TreatmentException(TreatmentError.PHOTO_LIMIT_EXCEEDED);
        }
        List<TreatmentPhoto> attached = new ArrayList<>(photos);
        attached.add(photo);
        return new TreatmentRecord(
                recordId, userId, serviceTypes, salonName, designerName, performedAt,
                satisfaction, priceAmount, priceCurrency, appointmentId,
                memo, nextVisitCautions, attached, createdAt);
    }

    /** 부분 수정에서 결정된 최종 값으로 기록의 새 스냅샷을 만든다. */
    public TreatmentRecord update(
            Set<ServiceType> serviceTypes,
            String salonName,
            String designerName,
            Instant performedAt,
            Integer satisfaction,
            Long priceAmount,
            String priceCurrency,
            UUID appointmentId,
            String memo,
            String nextVisitCautions
    ) {
        return new TreatmentRecord(
                recordId, userId, serviceTypes, salonName, designerName, performedAt,
                satisfaction, priceAmount, priceCurrency, appointmentId,
                memo, nextVisitCautions, photos, createdAt);
    }

    private static String normalizeName(String value, int maxLength, TreatmentError tooLong) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new TreatmentException(tooLong);
        }
        return trimmed;
    }

    private static String normalizeCurrency(String currency) {
        String upper = currency.strip().toUpperCase();
        if (!upper.matches("[A-Z]{3}")) {
            throw new TreatmentException(TreatmentError.PRICE_CURRENCY_INVALID);
        }
        return upper;
    }

    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
