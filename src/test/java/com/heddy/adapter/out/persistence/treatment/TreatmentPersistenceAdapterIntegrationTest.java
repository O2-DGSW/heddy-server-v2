package com.heddy.adapter.out.persistence.treatment;

import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.model.TreatmentPhoto;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.model.TreatmentRecordFilter;
import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TreatmentPersistenceAdapterIntegrationTest extends PostgresIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("81000000-0000-4000-8000-000000000001");
    private static final Instant PERFORMED_AT = Instant.parse("2026-08-01T10:00:00Z");

    @Autowired TreatmentPersistenceAdapter adapter;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpOwner() {
        insertUser(USER_ID);
    }

    // ------------------------------------------------------------------ 왕복

    @Test
    void savesRecordAndReadsItBackWithEveryField() {
        UUID appointmentId = UUID.randomUUID();
        TreatmentRecord record = TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.COLOR, ServiceType.PERM), "준헤어", "김실장",
                PERFORMED_AT, 4, 120_000L, "KRW", appointmentId);

        UUID recordId = adapter.insert(record).recordId();

        TreatmentRecord found = adapter.findByIdAndUserId(recordId, USER_ID).orElseThrow();
        assertThat(found.recordId()).isEqualTo(recordId);
        assertThat(found.userId()).isEqualTo(USER_ID);
        assertThat(found.serviceTypes()).containsExactlyInAnyOrder(ServiceType.COLOR, ServiceType.PERM);
        assertThat(found.salonName()).isEqualTo("준헤어");
        assertThat(found.designerName()).isEqualTo("김실장");
        assertThat(found.performedAt()).isEqualTo(PERFORMED_AT);
        assertThat(found.satisfaction()).isEqualTo(4);
        assertThat(found.priceAmount()).isEqualTo(120_000L);
        assertThat(found.priceCurrency()).isEqualTo("KRW");
        // appointment_id 는 FK 없는 일반 컬럼이다. 값이 그대로 왕복되는지만 확인한다.
        assertThat(found.appointmentId()).isEqualTo(appointmentId);
        assertThat(found.photos()).isEmpty();
        assertThat(found.createdAt()).isNotNull();
    }

    @Test
    void savesRecordWithPhotosAndReadsThemBackOrdered() {
        TreatmentRecord record = TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                null, null, null, null);
        TreatmentRecord saved = adapter.insert(record);
        TreatmentPhoto after = adapter.insertPhoto(
                TreatmentPhoto.create(saved.recordId(), newFile(), ImageType.AFTER, 5));
        TreatmentPhoto before = adapter.insertPhoto(
                TreatmentPhoto.create(saved.recordId(), newFile(), ImageType.BEFORE, 1));

        TreatmentRecord found = adapter.findByIdAndUserId(saved.recordId(), USER_ID).orElseThrow();

        assertThat(found.photos())
                .extracting(TreatmentPhoto::photoId)
                .containsExactly(before.photoId(), after.photoId());
        assertThat(found.photos())
                .extracting(TreatmentPhoto::imageType)
                .containsExactly(ImageType.BEFORE, ImageType.AFTER);
        assertThat(found.photos())
                .extracting(TreatmentPhoto::sortOrder)
                .containsExactly(1, 5);
        assertThat(found.photos().get(0).createdAt()).isNotNull();
    }

    @Test
    void updatesAndDeletesPhotoIndependently() {
        TreatmentRecord record = adapter.insert(TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                null, null, null, null));
        TreatmentPhoto photo = adapter.insertPhoto(TreatmentPhoto.create(
                record.recordId(), newFile(), ImageType.BEFORE, 0));

        TreatmentPhoto updated = adapter.updatePhoto(
                photo.update(ImageType.AFTER, 7)).orElseThrow();

        assertThat(updated.imageType()).isEqualTo(ImageType.AFTER);
        assertThat(updated.sortOrder()).isEqualTo(7);
        assertThat(adapter.findById(record.recordId()).orElseThrow().photos())
                .extracting(TreatmentPhoto::sortOrder)
                .containsExactly(7);
        assertThat(adapter.deletePhoto(photo.photoId())).isTrue();
        assertThat(adapter.findById(record.recordId()).orElseThrow().photos()).isEmpty();
        assertThat(adapter.deletePhoto(photo.photoId())).isFalse();
    }

    @Test
    void keepsOptionalColumnsEmptyWhenAbsent() {
        TreatmentRecord record = TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.OTHER), "", "  ", PERFORMED_AT,
                null, null, null, null);

        TreatmentRecord found = adapter
                .findByIdAndUserId(adapter.insert(record).recordId(), USER_ID).orElseThrow();

        assertThat(found.salonName()).isNull();
        assertThat(found.designerName()).isNull();
        assertThat(found.satisfaction()).isNull();
        assertThat(found.priceAmount()).isNull();
        assertThat(found.priceCurrency()).isNull();
        assertThat(found.appointmentId()).isNull();
    }

    @Test
    void storesServiceTypesAsJsonbArrayOfNames() {
        TreatmentRecord record = TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.CUT, ServiceType.COLOR), null, null,
                PERFORMED_AT, null, null, null, null);

        UUID recordId = adapter.insert(record).recordId();
        String stored = jdbcTemplate.queryForObject(
                "SELECT service_types::text FROM treatment_records WHERE record_id = ?",
                String.class, recordId);

        assertThat(stored).startsWith("[").endsWith("]").contains("\"CUT\"", "\"COLOR\"");
    }

    @Test
    void returnsEmptyOptionalForUnknownRecord() {
        assertThat(adapter.findByIdAndUserId(UUID.randomUUID(), USER_ID)).isEmpty();
    }

    /** 소유자 조건은 질의에 실린다 — 남의 기록은 사진을 읽기도 전에 빈 값이 된다. */
    @Test
    void returnsEmptyOptionalWhenTheRecordBelongsToSomeoneElse() {
        TreatmentRecord saved = adapter.insert(TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                null, null, null, null));
        adapter.insertPhoto(TreatmentPhoto.create(saved.recordId(), newFile(), ImageType.BEFORE));

        assertThat(adapter.findByIdAndUserId(saved.recordId(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void findsAFilteredPageInStablePerformedAtOrder() {
        TreatmentRecord older = adapter.insert(TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.CUT), "준헤어", "김실장",
                PERFORMED_AT.minusSeconds(3_600), null, null, null, null));
        TreatmentRecord newer = adapter.insert(TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.CUT, ServiceType.COLOR), "준헤어", "김실장",
                PERFORMED_AT, null, null, null, null));
        adapter.insert(TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.PERM), "다른미용실", "다른디자이너",
                PERFORMED_AT.minusSeconds(1_800), null, null, null, null));

        var firstPage = adapter.findPage(new TreatmentRecordFilter(
                USER_ID, ServiceType.CUT, "김실장", "준헤어",
                PERFORMED_AT.minusSeconds(7_200), PERFORMED_AT.plusSeconds(1),
                0, 1, false));
        var secondPage = adapter.findPage(new TreatmentRecordFilter(
                USER_ID, ServiceType.CUT, "김실장", "준헤어",
                PERFORMED_AT.minusSeconds(7_200), PERFORMED_AT.plusSeconds(1),
                1, 1, false));

        assertThat(firstPage.totalElements()).isEqualTo(2);
        assertThat(firstPage.items()).extracting(TreatmentRecord::recordId)
                .containsExactly(newer.recordId());
        assertThat(secondPage.items()).extracting(TreatmentRecord::recordId)
                .containsExactly(older.recordId());
    }

    @Test
    void updatesMutableFieldsAndKeepsPhotosAttached() {
        TreatmentRecord saved = adapter.insert(TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.CUT), "기존 미용실", "기존 디자이너",
                PERFORMED_AT, 3, 100_000L, "KRW", null, "기존 메모", "기존 주의사항"));
        TreatmentPhoto photo = adapter.insertPhoto(
                TreatmentPhoto.create(saved.recordId(), newFile(), ImageType.AFTER));
        TreatmentRecord withPhoto = adapter
                .findByIdAndUserId(saved.recordId(), USER_ID).orElseThrow();
        TreatmentRecord changed = withPhoto.update(
                Set.of(ServiceType.COLOR), "새 미용실", "새 디자이너",
                PERFORMED_AT.minusSeconds(60), 5, null, null, null,
                "새 메모", null);

        TreatmentRecord updated = adapter.update(changed).orElseThrow();

        assertThat(updated.serviceTypes()).containsExactly(ServiceType.COLOR);
        assertThat(updated.salonName()).isEqualTo("새 미용실");
        assertThat(updated.designerName()).isEqualTo("새 디자이너");
        assertThat(updated.satisfaction()).isEqualTo(5);
        assertThat(updated.priceAmount()).isNull();
        assertThat(updated.memo()).isEqualTo("새 메모");
        assertThat(updated.nextVisitCautions()).isNull();
        assertThat(updated.photos()).extracting(TreatmentPhoto::photoId)
                .containsExactly(photo.photoId());
    }

    @Test
    void hardDeletesRecordAndCascadesPhotos() {
        TreatmentRecord saved = adapter.insert(TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                null, null, null, null));
        adapter.insertPhoto(TreatmentPhoto.create(
                saved.recordId(), newFile(), ImageType.AFTER));

        assertThat(adapter.deleteById(saved.recordId())).isTrue();
        assertThat(adapter.findByIdAndUserId(saved.recordId(), USER_ID)).isEmpty();
        assertThat(photoCountOf(saved.recordId())).isZero();
        assertThat(adapter.deleteById(saved.recordId())).isFalse();
    }

    // ------------------------------------------------------------------ 참조 무결성

    @Test
    void rejectsRecordOfUnknownUser() {
        TreatmentRecord orphan = TreatmentRecord.create(
                UUID.randomUUID(), Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                null, null, null, null);

        assertThatThrownBy(() -> adapter.insert(orphan))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsPhotoOfUnknownFile() {
        TreatmentRecord saved = adapter.insert(TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                null, null, null, null));
        TreatmentPhoto ghostFilePhoto = TreatmentPhoto.create(
                saved.recordId(), UUID.randomUUID(), ImageType.BEFORE);

        assertThatThrownBy(() -> adapter.insertPhoto(ghostFilePhoto))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cascadesPhotoRowsWhenRecordIsDeletedButRestrictsUserDeletion() {
        TreatmentRecord saved = adapter.insert(TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                null, null, null, null));
        adapter.insertPhoto(TreatmentPhoto.create(saved.recordId(), newFile(), ImageType.AFTER));

        jdbcTemplate.update("DELETE FROM treatment_records WHERE record_id = ?", saved.recordId());
        assertThat(photoCountOf(saved.recordId())).isZero();

        // 사용자 삭제는 막힌다(files 와 같은 판단). 사진의 file_id 를 잃으면 스토리지 객체를 회수할 수 없다.
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", USER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ------------------------------------------------------------------ 스키마 대조

    @Test
    void treatmentRecordsTableMatchesMigration() {
        assertColumn("treatment_records", "record_id", "uuid", null, false);
        assertColumn("treatment_records", "user_id", "uuid", null, false);
        assertColumn("treatment_records", "service_types", "jsonb", null, false);
        assertColumn("treatment_records", "salon_name", "character varying", 50, true);
        assertColumn("treatment_records", "designer_name", "character varying", 30, true);
        assertColumn("treatment_records", "performed_at", "timestamp with time zone", null, false);
        assertColumn("treatment_records", "satisfaction", "smallint", null, true);
        assertColumn("treatment_records", "price_amount", "bigint", null, true);
        assertColumn("treatment_records", "price_currency", "character varying", 3, true);
        assertColumn("treatment_records", "appointment_id", "uuid", null, true);
        assertColumn("treatment_records", "memo", "text", null, true);
        assertColumn("treatment_records", "next_visit_cautions", "text", null, true);
        assertColumn("treatment_records", "created_at", "timestamp with time zone", null, false);
        assertColumn("treatment_records", "updated_at", "timestamp with time zone", null, false);
    }

    @Test
    void treatmentRecordPhotosTableMatchesMigration() {
        assertColumn("treatment_record_photos", "photo_id", "uuid", null, false);
        assertColumn("treatment_record_photos", "record_id", "uuid", null, false);
        assertColumn("treatment_record_photos", "file_id", "uuid", null, false);
        assertColumn("treatment_record_photos", "image_type", "character varying", 20, false);
        assertColumn("treatment_record_photos", "sort_order", "integer", null, false);
        assertColumn("treatment_record_photos", "created_at", "timestamp with time zone", null, false);
        assertColumn("treatment_record_photos", "updated_at", "timestamp with time zone", null, false);
    }

    @Test
    void keepsIndexesThatTimelineFilterAndCleanupQueriesDependOn() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'treatment_records'", String.class))
                .contains("idx_treatment_records_user_performed", "idx_treatment_records_service_types");

        assertThat(jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'treatment_record_photos'", String.class))
                .contains("idx_treatment_record_photos_record_id", "idx_treatment_record_photos_file_id",
                        "idx_treatment_record_photos_record_sort");

        // 시술 종류 필터 질의가 GIN 을 타는지 — 인덱스 방식까지 확인한다.
        String accessMethod = jdbcTemplate.queryForObject("""
                SELECT am.amname FROM pg_indexes i
                JOIN pg_class c ON c.relname = i.indexname
                JOIN pg_am am ON am.oid = c.relam
                WHERE i.tablename = 'treatment_records' AND i.indexname = 'idx_treatment_records_service_types'
                """, String.class);
        assertThat(accessMethod).isEqualTo("gin");

        jdbcTemplate.execute("SET LOCAL enable_seqscan = off");
        String plan = String.join("\n", jdbcTemplate.queryForList("""
                EXPLAIN SELECT record_id
                FROM treatment_records
                WHERE service_types @> '["CUT"]'::jsonb
                """, String.class));
        assertThat(plan).contains("idx_treatment_records_service_types");
    }

    // ------------------------------------------------------------------ 헬퍼

    private void insertUser(UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, email, password_hash, auth_provider, status, login_fail_count
                ) VALUES (?, ?, ?, 'EMAIL', 'ACTIVE', 0)
                """, userId, "treatment-owner@example.com", "hash");
    }

    private UUID newFile() {
        UUID fileId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO files (
                    file_id, upload_id, user_id, purpose, status, object_key,
                    content_type, file_size, expires_at
                ) VALUES (?, ?, ?, 'TREATMENT_PHOTO', 'READY', ?, 'image/jpeg', 1024, now() + interval '5 minutes')
                """, fileId, UUID.randomUUID(), USER_ID, "TREATMENT_PHOTO/" + USER_ID + "/" + fileId);
        return fileId;
    }

    private int photoCountOf(UUID recordId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM treatment_record_photos WHERE record_id = ?",
                Integer.class, recordId);
        return count == null ? 0 : count;
    }

    private void assertColumn(String table, String name, String dataType, Integer length, boolean nullable) {
        var column = jdbcTemplate.queryForMap("""
                SELECT data_type, character_maximum_length, is_nullable
                FROM information_schema.columns
                WHERE table_name = ? AND column_name = ?
                """, table, name);

        assertThat(column.get("data_type")).as("%s.%s 타입", table, name).isEqualTo(dataType);
        assertThat(column.get("character_maximum_length")).as("%s.%s 길이", table, name).isEqualTo(length);
        assertThat(column.get("is_nullable")).as("%s.%s nullable", table, name)
                .isEqualTo(nullable ? "YES" : "NO");
    }
}
