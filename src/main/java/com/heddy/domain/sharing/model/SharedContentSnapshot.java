package com.heddy.domain.sharing.model;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@link com.heddy.domain.sharing.port.out.SharedContentPort} 가 돌려주는 원시 스냅샷.
 * 식별자가 그대로 들어 있으므로 이 타입은 도메인 밖으로 새지 않는다 — 유스케이스가
 * {@link SharedContentView} 로 바꿔 내보낸다.
 */
public record SharedContentSnapshot(
        String ownerDisplayName,
        List<RecordSnapshot> records
) {

    public record RecordSnapshot(
            Instant performedAt,
            String salonName,
            String designerName,
            Set<String> serviceTypes,
            Integer satisfaction,
            String memo,
            String nextVisitCautions,
            List<PhotoSnapshot> photos
    ) {
    }

    /** 사진은 파일 식별자와 상태만 실려 온다. URL 발급은 조회 시점에 이루어진다. */
    public record PhotoSnapshot(
            String imageType,
            UUID fileId,
            boolean ready
    ) {
    }
}
