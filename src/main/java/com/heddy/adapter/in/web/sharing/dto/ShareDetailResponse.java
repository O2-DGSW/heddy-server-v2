package com.heddy.adapter.in.web.sharing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.sharing.model.Share;
import com.heddy.domain.sharing.model.ShareFieldType;
import com.heddy.domain.sharing.model.ShareStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 공유 설정 상세 응답(소유자 전용). 대상 식별자까지 보여주는 것이 목록과 다른 점이라
 * 공개 응답(#51)과 DTO 를 아예 분리한다.
 */
public record ShareDetailResponse(
        @Schema(description = "공유 식별자")
        @JsonProperty("share_id") UUID shareId,

        @Schema(description = "상태")
        ShareStatus status,

        @Schema(description = "노출 항목")
        Set<ShareFieldType> fields,

        @Schema(description = "공유된 시술기록 식별자")
        @JsonProperty("record_ids") Set<UUID> recordIds,

        @Schema(description = "공유된 후보 스타일 식별자")
        @JsonProperty("saved_style_ids") Set<UUID> savedStyleIds,

        @Schema(description = "만료 시각")
        @JsonProperty("expires_at") Instant expiresAt,

        @Schema(description = "철회 시각. 철회하지 않았으면 비어 있다")
        @JsonProperty("revoked_at") Instant revokedAt,

        @Schema(description = "생성 시각")
        @JsonProperty("created_at") Instant createdAt
) {

    public static ShareDetailResponse from(Share share) {
        return new ShareDetailResponse(share.shareId(), share.status(), share.fields(),
                share.recordIds(), share.savedStyleIds(), share.expiresAt(),
                share.revokedAt(), share.createdAt());
    }
}
