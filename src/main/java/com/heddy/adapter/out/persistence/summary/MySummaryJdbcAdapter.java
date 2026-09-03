package com.heddy.adapter.out.persistence.summary;

import com.heddy.domain.sharing.model.ShareStatus;
import com.heddy.domain.summary.model.MySummary;
import com.heddy.domain.summary.port.out.MySummaryQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * 카운트 4종을 스칼라 서브쿼리 하나로 묶어 왕복 한 번에 끝낸다. 도메인별 리포지토리를 네 번
 * 호출하면 화면 하나에 질의가 네 번 나가고, 정의도 네 곳에 흩어진다.
 *
 * <p>타 도메인 테이블을 직접 읽는 읽기 전용 어댑터다 — 공개 조회의
 * {@code SharedContentJdbcAdapter} 와 같은 이유로, 집계만 하는 질의가 다른 도메인의
 * 영속성 내부를 재사용할 이유가 없다(타 도메인 리포지토리는 패키지 비공개다).
 */
@Component
@RequiredArgsConstructor
public class MySummaryJdbcAdapter implements MySummaryQueryPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public MySummary count(UUID userId, Instant now) {
        // 분석·공유는 record 기준 distinct 다. 한 기록에 성공 분석이 여러 건일 수 있고,
        // 한 기록이 여러 공유 링크에 들어갈 수도 있어 행을 세면 기록 수보다 커진다.
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT count(*) FROM treatment_records WHERE user_id = ?)
                        AS treatment_record_count,
                    (SELECT count(DISTINCT record_id) FROM analysis_results WHERE user_id = ?)
                        AS analyzed_record_count,
                    (SELECT count(*) FROM saved_styles WHERE user_id = ?)
                        AS saved_style_count,
                    (SELECT count(DISTINCT sr.record_id)
                       FROM share_records sr
                       JOIN shares s ON s.share_id = sr.share_id
                      WHERE s.user_id = ? AND s.status = ? AND s.expires_at > ?)
                        AS shared_record_count
                """,
                (rs, rowNum) -> new MySummary(
                        rs.getLong("treatment_record_count"),
                        rs.getLong("analyzed_record_count"),
                        rs.getLong("saved_style_count"),
                        rs.getLong("shared_record_count")),
                userId, userId, userId, userId, ShareStatus.ACTIVE.name(), Timestamp.from(now));
    }
}
