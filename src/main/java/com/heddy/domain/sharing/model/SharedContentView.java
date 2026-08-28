package com.heddy.domain.sharing.model;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 공개 조회용 읽기 전용 스냅샷. 인증된 소유자 응답과 DTO 를 아예 분리하는 재료다 —
 * user_id·record_id 같은 내부 식별자는 이 안에도 들어가지 않는다(스펙 11.6).
 * 공유에서 선택하지 않은 항목의 값은 null 로 두고, 직렬화 단계에서 빠진다.
 */
public record SharedContentView(
        String ownerDisplayName,
        List<SharedRecordView> records,
        List<SharedSavedStyleView> savedStyles
) {

    /** 선택 항목 게이트를 통과한 값만 채워진다. 나머지는 null 이다. */
    public record SharedRecordView(
            Instant performedAt,
            String salonName,
            String designerName,
            Set<String> serviceTypes,
            Integer satisfaction,
            String memo,
            String nextVisitCautions,
            List<SharedPhotoView> photos
    ) {
    }

    /**
     * 조회 시점에 짧게 만드는 URL 만 담는다. object key 는 노출 대상이 아니고, 다운로드 전용
     * URL 도 제공하지 않는다 — 보여주기 위한 GET 이 전부다.
     */
    public record SharedPhotoView(
            String imageType,
            URI displayUrl
    ) {
    }

    /** 공개 화면에 필요한 저장 후보 값만 담고 사용자·저장 스타일 식별자는 제외한다. */
    public record SharedSavedStyleView(
            String styleName,
            String imageUrl,
            String reason
    ) {
    }
}
