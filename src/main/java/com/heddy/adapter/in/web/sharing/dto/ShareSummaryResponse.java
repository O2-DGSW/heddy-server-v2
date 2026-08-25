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
 * 내 공유 목록 항목. 토큰 해시만 남은 행에서 만들어지므로 링크 URL 은 애초에 존재하지 않는다.
 */
public record ShareSummaryResponse(
        @Schema(description = "공유 식별자")
        @JsonProperty("share_id") UUID shareId,

        @Schema(description = "상태")
        ShareStatus status,

        @Schema(description = "노출 항목")
        Set<ShareFieldType> fields,

        @Schema(description = "만료 시각")
        @JsonProperty("expires_at") Instant expiresAt,

        @Schema(description = "생성 시각")
        @JsonProperty("created_at") Instant createdAt
) {

    public static ShareSummaryResponse from(Share share) {
        return new ShareSummaryResponse(share.shareId(), share.status(), share.fields(),
                share.expiresAt(), share.createdAt());
    }
}
