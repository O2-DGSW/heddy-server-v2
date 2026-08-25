package com.heddy.adapter.in.web.sharing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.sharing.model.ShareFieldType;
import com.heddy.domain.sharing.model.ShareStatus;
import com.heddy.domain.sharing.port.in.CreateShareUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 공유 생성 응답. share_url 에 담긴 토큰 원문은 이 응답으로만 내보내고 서버는 저장하지 않으므로,
 * 목록·상세 어디에서도 다시 볼 수 없다.
 */
public record ShareResponse(
        @Schema(description = "공유 식별자")
        @JsonProperty("share_id") UUID shareId,

        @Schema(description = "공유 링크. 토큰 원문을 포함해 생성 시점에만 내려간다")
        @JsonProperty("share_url") String shareUrl,

        @Schema(description = "상태")
        ShareStatus status,

        @Schema(description = "노출 항목")
        Set<ShareFieldType> fields,

        @Schema(description = "만료 시각")
        @JsonProperty("expires_at") Instant expiresAt,

        @Schema(description = "생성 시각")
        @JsonProperty("created_at") Instant createdAt
) {

    public static ShareResponse from(CreateShareUseCase.Result result) {
        return new ShareResponse(result.share().shareId(), result.shareUrl(),
                result.share().status(), result.share().fields(),
                result.share().expiresAt(), result.share().createdAt());
    }
}
