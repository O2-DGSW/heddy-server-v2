package com.heddy.adapter.out.persistence.sharing;

import com.heddy.domain.sharing.model.SharedContentSnapshot;
import com.heddy.domain.sharing.model.SharedContentSnapshot.PhotoSnapshot;
import com.heddy.domain.sharing.model.SharedContentSnapshot.RecordSnapshot;
import com.heddy.domain.sharing.port.out.SharedContentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 공개 조회용 읽기 전용 질의. 시술기록·사진·프로필 테이블을 소유자 조건 없이 직접 읽는 유일한
 * 어댑터다 — 호출부가 이미 토큰 해시로 소유자를 특정했고, 읽기 전용이라 다른 도메인의
 * 영속성 내부를 재사용할 이유가 없다(타 도메인 리포지토리는 패키지 비공개다).
 */
@Component
@RequiredArgsConstructor
public class SharedContentJdbcAdapter implements SharedContentPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public SharedContentSnapshot load(UUID ownerId, Set<UUID> recordIds) {
        if (recordIds.isEmpty()) {
            return new SharedContentSnapshot(ownerName(ownerId), List.of());
        }
        // 기록이 삭제돼 사라졌어도 링크 전체를 깨지 않는다. 남은 기록만 보여준다.
        List<RecordSnapshot> records = new ArrayList<>();
        for (UUID recordId : orderNewestFirst(recordIds)) {
            RecordSnapshot record = loadRecord(recordId);
            if (record != null) {
                records.add(record);
            }
        }
        return new SharedContentSnapshot(ownerName(ownerId), List.copyOf(records));
    }

    /** 타임라인과 같은 최신순으로 보여준다. */
    private List<UUID> orderNewestFirst(Set<UUID> recordIds) {
        String placeholders = String.join(",", recordIds.stream().map(id -> "?").toList());
        List<Object> ids = List.copyOf(recordIds);
        return jdbcTemplate.query(
                "SELECT record_id FROM treatment_records WHERE record_id IN (" + placeholders
                        + ") ORDER BY performed_at DESC",
                (rs, i) -> rs.getObject("record_id", UUID.class),
                ids.toArray());
    }

    private RecordSnapshot loadRecord(UUID recordId) {
        // service_types 는 SQL 에서 바로 text 배열로 푼다. jsonb 를 애플리케이션에서 다시
        // 파싱하는 것보다 질의 한 줄이 끝이다.
        List<RecordSnapshot> found = jdbcTemplate.query("""
                SELECT performed_at, salon_name, designer_name,
                       (SELECT array_agg(value) FROM jsonb_array_elements_text(
                            treatment_records.service_types)) AS service_types,
                       satisfaction, memo, next_visit_cautions
                FROM treatment_records
                WHERE record_id = ?
                """, (rs, i) -> {
                    Array serviceTypes = rs.getArray("service_types");
                    return new RecordSnapshot(
                            rs.getObject("performed_at", Timestamp.class).toInstant(),
                            rs.getString("salon_name"),
                            rs.getString("designer_name"),
                            serviceTypes == null ? Set.of()
                                    : Set.copyOf(Arrays.asList((String[]) serviceTypes.getArray())),
                            rs.getObject("satisfaction", Integer.class),
                            rs.getString("memo"),
                            rs.getString("next_visit_cautions"),
                            photosOf(recordId));
                },
                recordId);
        return found.isEmpty() ? null : found.getFirst();
    }

    private List<PhotoSnapshot> photosOf(UUID recordId) {
        return jdbcTemplate.query("""
                SELECT p.image_type, f.file_id, f.status
                FROM treatment_record_photos p
                JOIN files f ON f.file_id = p.file_id
                WHERE p.record_id = ?
                ORDER BY p.sort_order, p.created_at, p.photo_id
                """, (rs, i) -> new PhotoSnapshot(
                        rs.getString("image_type"),
                        rs.getObject("file_id", UUID.class),
                        "READY".equals(rs.getString("status"))),
                recordId);
    }

    private String ownerName(UUID ownerId) {
        // 닉네임은 NOT NULL 이지만 프로필 행이 아직 없는 계정도 있을 수 있다. 그때는 이름 대신
        // 고정 문구를 쓴다 — 빈 값을 내보내 링크가 깨져 보이게 하지 않는다.
        List<String> names = jdbcTemplate.queryForList(
                "SELECT nickname FROM user_profiles WHERE user_id = ?",
                String.class, ownerId);
        return names.isEmpty() || names.getFirst() == null
                ? "헤디 사용자"
                : names.getFirst();
    }
}
