package com.heddy.domain.treatment.model;

import com.heddy.domain.treatment.exception.TreatmentError;
import com.heddy.domain.treatment.exception.TreatmentException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TreatmentRecordTest {

    private static final Instant PERFORMED_AT = Instant.parse("2026-08-01T10:00:00Z");

    @Test
    void createIssuesIdentityAndStartsWithNoPhotos() {
        UUID userId = UUID.randomUUID();

        TreatmentRecord record = TreatmentRecord.create(
                userId, Set.of(ServiceType.CUT), "준헤어", "김실장",
                PERFORMED_AT, 5, 80_000L, "KRW", null);

        assertThat(record.recordId()).isNotNull();
        assertThat(record.userId()).isEqualTo(userId);
        assertThat(record.serviceTypes()).containsExactly(ServiceType.CUT);
        assertThat(record.photos()).isEmpty();
        assertThat(record.createdAt()).isNull();
    }

    @Test
    void keepsEveryGivenValue() {
        TreatmentRecord record = record(Set.of(ServiceType.COLOR, ServiceType.PERM));

        assertThat(record.salonName()).isEqualTo("준헤어");
        assertThat(record.designerName()).isEqualTo("김실장");
        assertThat(record.performedAt()).isEqualTo(PERFORMED_AT);
        assertThat(record.satisfaction()).isEqualTo(4);
        assertThat(record.priceAmount()).isEqualTo(120_000L);
        assertThat(record.priceCurrency()).isEqualTo("KRW");
        assertThat(record.appointmentId()).isEqualTo(APPOINTMENT_ID);
    }

    // ------------------------------------------------------------------ 시술 종류

    @Test
    void rejectsRecordWithoutServiceType() {
        assertThatThrownBy(() -> record(Set.of()))
                .isInstanceOf(TreatmentException.class)
                .extracting(e -> ((TreatmentException) e).error())
                .isEqualTo(TreatmentError.SERVICE_TYPE_REQUIRED);
    }

    // ------------------------------------------------------------------ 시술일

    @Test
    void rejectsFuturePerformedAt() {
        Instant tomorrow = Instant.now().plusSeconds(86_400);

        assertThatThrownBy(() -> recordWithPerformedAt(tomorrow))
                .isInstanceOf(TreatmentException.class)
                .extracting(e -> ((TreatmentException) e).error())
                .isEqualTo(TreatmentError.PERFORMED_AT_IN_FUTURE);
    }

    @Test
    void acceptsJustHappenedPerformedAt() {
        TreatmentRecord record = recordWithPerformedAt(Instant.now().minusSeconds(60));

        assertThat(record.performedAt()).isNotNull();
    }

    // ------------------------------------------------------------------ 만족도

    @Test
    void rejectsSatisfactionOutOfRange() {
        for (int invalid : new int[] {0, 6, -1}) {
            assertThatThrownBy(() -> recordWithSatisfaction(invalid))
                    .as("만족도 %d", invalid)
                    .isInstanceOf(TreatmentException.class)
                    .extracting(e -> ((TreatmentException) e).error())
                    .isEqualTo(TreatmentError.SATISFACTION_OUT_OF_RANGE);
        }
    }

    @Test
    void acceptsBoundarySatisfactionsAndOmission() {
        assertThat(recordWithSatisfaction(1).satisfaction()).isEqualTo(1);
        assertThat(recordWithSatisfaction(5).satisfaction()).isEqualTo(5);
        assertThat(recordWithSatisfaction(null).satisfaction()).isNull();
    }

    // ------------------------------------------------------------------ 가격

    @Test
    void rejectsAmountWithoutCurrencyAndViceVersa() {
        assertThatThrownBy(() -> record(null, "KRW"))
                .isInstanceOf(TreatmentException.class)
                .extracting(e -> ((TreatmentException) e).error())
                .isEqualTo(TreatmentError.PRICE_INCOMPLETE);

        assertThatThrownBy(() -> record(50_000L, null))
                .isInstanceOf(TreatmentException.class)
                .extracting(e -> ((TreatmentException) e).error())
                .isEqualTo(TreatmentError.PRICE_INCOMPLETE);
    }

    @Test
    void rejectsNegativePrice() {
        assertThatThrownBy(() -> record(-1L, "KRW"))
                .isInstanceOf(TreatmentException.class)
                .extracting(e -> ((TreatmentException) e).error())
                .isEqualTo(TreatmentError.PRICE_AMOUNT_NEGATIVE);
    }

    @Test
    void normalizesCurrencyToUppercaseThreeLetters() {
        assertThat(record(50_000L, "krw").priceCurrency()).isEqualTo("KRW");
    }

    @Test
    void rejectsMalformedCurrency() {
        for (String malformed : new String[] {"KR", "KWON", "K1W", ""}) {
            assertThatThrownBy(() -> record(50_000L, malformed))
                    .as("통화 코드 %s", malformed)
                    .isInstanceOf(TreatmentException.class)
                    .extracting(e -> ((TreatmentException) e).error())
                    .isEqualTo(TreatmentError.PRICE_CURRENCY_INVALID);
        }
    }

    @Test
    void acceptsZeroPriceAsFreeOrEventTreatment() {
        assertThat(record(0L, "KRW").priceAmount()).isZero();
    }

    // ------------------------------------------------------------------ 이름 필드

    @Test
    void rejectsSalonNameLongerThanColumn() {
        String fiftyOneChars = "가".repeat(51);

        assertThatThrownBy(() -> recordWithSalonName(fiftyOneChars))
                .isInstanceOf(TreatmentException.class)
                .extracting(e -> ((TreatmentException) e).error())
                .isEqualTo(TreatmentError.SALON_NAME_TOO_LONG);
    }

    @Test
    void rejectsDesignerNameLongerThanColumn() {
        String thirtyOneChars = "디".repeat(31);

        assertThatThrownBy(() -> recordWithDesignerName(thirtyOneChars))
                .isInstanceOf(TreatmentException.class)
                .extracting(e -> ((TreatmentException) e).error())
                .isEqualTo(TreatmentError.DESIGNER_NAME_TOO_LONG);
    }

    @Test
    void treatsBlankNamesAsAbsent() {
        TreatmentRecord record = TreatmentRecord.create(
                UUID.randomUUID(), Set.of(ServiceType.CUT), "   ", "", PERFORMED_AT,
                null, null, null, null);

        assertThat(record.salonName()).isNull();
        assertThat(record.designerName()).isNull();
    }

    @Test
    void stripsSurroundingWhitespaceOfNames() {
        TreatmentRecord record = recordWithSalonName("  준헤어  ");

        assertThat(record.salonName()).isEqualTo("준헤어");
    }

    // ------------------------------------------------------------------ 사진 불변식

    @Test
    void attachPhotoAppendsPhoto() {
        TreatmentRecord record = record(Set.of(ServiceType.CUT));
        TreatmentPhoto before = photoFor(record);

        TreatmentRecord attached = record.attachPhoto(before);

        assertThat(attached.photos()).hasSize(1);
        assertThat(attached.photos().get(0)).isEqualTo(before);
    }

    @Test
    void acceptsExactlyTenPhotosAsTheBoundary() {
        TreatmentRecord full = fullTenPhotos();

        assertThat(full.photos()).hasSize(TreatmentRecord.MAX_PHOTOS);
    }

    @Test
    void refusesEleventhPhoto() {
        TreatmentRecord full = fullTenPhotos();

        assertThatThrownBy(() -> full.attachPhoto(photoFor(full)))
                .isInstanceOf(TreatmentException.class)
                .extracting(e -> ((TreatmentException) e).error())
                .isEqualTo(TreatmentError.PHOTO_LIMIT_EXCEEDED);
    }

    @Test
    void refusesPhotoBelongingToAnotherRecord() {
        TreatmentRecord record = record(Set.of(ServiceType.CUT));
        TreatmentPhoto foreign = TreatmentPhoto.create(
                UUID.randomUUID(), UUID.randomUUID(), ImageType.BEFORE);

        assertThatThrownBy(() -> record.attachPhoto(foreign))
                .isInstanceOf(TreatmentException.class)
                .extracting(e -> ((TreatmentException) e).error())
                .isEqualTo(TreatmentError.PHOTO_RECORD_MISMATCH);
    }

    @Test
    void refusesNegativePhotoSortOrder() {
        assertThatThrownBy(() -> TreatmentPhoto.create(
                UUID.randomUUID(), UUID.randomUUID(), ImageType.OTHER, -1))
                .isInstanceOf(TreatmentException.class)
                .extracting(error -> ((TreatmentException) error).error())
                .isEqualTo(TreatmentError.PHOTO_SORT_ORDER_NEGATIVE);
    }

    @Test
    void refusesRehydratedRecordCarryingMoreThanTenPhotos() {
        TreatmentRecord source = fullTenPhotos();
        List<TreatmentPhoto> eleven = new java.util.ArrayList<>(source.photos());
        eleven.add(TreatmentPhoto.create(source.recordId(), UUID.randomUUID(), ImageType.OTHER));

        assertThatThrownBy(() -> new TreatmentRecord(
                source.recordId(), source.userId(), source.serviceTypes(), source.salonName(),
                source.designerName(), source.performedAt(), source.satisfaction(),
                source.priceAmount(), source.priceCurrency(), source.appointmentId(),
                eleven, source.createdAt()))
                .isInstanceOf(TreatmentException.class)
                .extracting(e -> ((TreatmentException) e).error())
                .isEqualTo(TreatmentError.PHOTO_LIMIT_EXCEEDED);
    }

    // ------------------------------------------------------------------ 방어적 복사

    @Test
    void givenServiceTypesAreCopiedSoLaterMutationDoesNotLeakIn() {
        Set<ServiceType> mutable = new LinkedHashSet<>();
        mutable.add(ServiceType.CUT);
        TreatmentRecord record = TreatmentRecord.create(
                UUID.randomUUID(), mutable, null, null, PERFORMED_AT,
                null, null, null, null);
        mutable.add(ServiceType.COLOR);

        assertThat(record.serviceTypes()).containsExactly(ServiceType.CUT);
        assertThatThrownBy(() -> record.serviceTypes().add(ServiceType.PERM))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ------------------------------------------------------------------ 열거값 목록 고정

    @Test
    void serviceTypesCoverExactlyTheApiContractValues() {
        assertThat(ServiceType.values())
                .extracting(Enum::name)
                .containsExactly("CUT", "PERM", "COLOR", "BLEACH", "CLINIC", "STYLING", "OTHER");
    }

    @Test
    void imageTypesCoverExactlyTheApiContractValues() {
        assertThat(ImageType.values())
                .extracting(Enum::name)
                .containsExactly("BEFORE", "AFTER", "OTHER");
    }

    // ------------------------------------------------------------------ 헬퍼

    private static final UUID APPOINTMENT_ID = UUID.randomUUID();

    private static TreatmentRecord record(Set<ServiceType> serviceTypes) {
        return TreatmentRecord.create(
                UUID.randomUUID(), serviceTypes, "준헤어", "김실장", PERFORMED_AT,
                4, 120_000L, "KRW", APPOINTMENT_ID);
    }

    private static TreatmentRecord recordWithPerformedAt(Instant performedAt) {
        return TreatmentRecord.create(
                UUID.randomUUID(), Set.of(ServiceType.CUT), null, null, performedAt,
                null, null, null, null);
    }

    private static TreatmentRecord recordWithSatisfaction(Integer satisfaction) {
        return TreatmentRecord.create(
                UUID.randomUUID(), Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                satisfaction, null, null, null);
    }

    private static TreatmentRecord record(Long priceAmount, String priceCurrency) {
        return TreatmentRecord.create(
                UUID.randomUUID(), Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                null, priceAmount, priceCurrency, null);
    }

    private static TreatmentRecord recordWithSalonName(String salonName) {
        return TreatmentRecord.create(
                UUID.randomUUID(), Set.of(ServiceType.CUT), salonName, null, PERFORMED_AT,
                null, null, null, null);
    }

    private static TreatmentRecord recordWithDesignerName(String designerName) {
        return TreatmentRecord.create(
                UUID.randomUUID(), Set.of(ServiceType.CUT), null, designerName, PERFORMED_AT,
                null, null, null, null);
    }

    private static TreatmentPhoto photoFor(TreatmentRecord record) {
        return TreatmentPhoto.create(record.recordId(), UUID.randomUUID(), ImageType.BEFORE);
    }

    private static TreatmentRecord fullTenPhotos() {
        TreatmentRecord record = record(Set.of(ServiceType.CUT));
        for (int i = 0; i < TreatmentRecord.MAX_PHOTOS; i++) {
            record = record.attachPhoto(photoFor(record));
        }
        return record;
    }
}
