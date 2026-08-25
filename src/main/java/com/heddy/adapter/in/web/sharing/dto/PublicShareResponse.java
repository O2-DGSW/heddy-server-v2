package com.heddy.adapter.in.web.sharing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.sharing.model.SharedContentView.SharedPhotoView;
import com.heddy.domain.sharing.model.SharedContentView.SharedRecordView;
import com.heddy.domain.sharing.port.in.GetPublicShareUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * 공개 공유 응답. 인증된 소유자 응답과 DTO 를 아예 분리하고(스펙 19절), 미선택 필드는
 * {@code NON_NULL} 로 직렬화 단계부터 제외한다. 내부 식별자와 S3 object key 는 어디에도
 * 없고, 사진 URL 은 조회 시점에 짧게 발급된 GET 하나뿐이다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicShareResponse(
        @Schema(description = "링크 메타 정보")
        @JsonProperty("share") ShareHeader share,

        @Schema(description = "공유된 시술기록")
        @JsonProperty("records") List<PublicRecordResponse> records,

        @Schema(description = "공유된 후보 스타일. 항목을 선택했으면 지금은 빈 배열이다")
        @JsonProperty("saved_styles") List<PublicSavedStyleResponse> savedStyles
) {

    public static PublicShareResponse from(GetPublicShareUseCase.Result result) {
        return new PublicShareResponse(
                new ShareHeader(result.expiresAt(), result.content().ownerDisplayName()),
                result.content().records().stream().map(PublicRecordResponse::from).toList(),
                result.includesSavedStyles() ? List.of() : null);
    }

    public record ShareHeader(
            @JsonProperty("expires_at") Instant expiresAt,

            @Schema(description = "공유한 사람의 표시 이름. user_id 는 노출하지 않는다")
            @JsonProperty("owner_display_name") String ownerDisplayName
    ) {
    }

    /** 미선택 항목은 값이 아니라 키 자체가 빠진다. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PublicRecordResponse(
            @JsonProperty("performed_at") Instant performedAt,
            @JsonProperty("salon_name") String salonName,
            @JsonProperty("designer_name") String designerName,
            @JsonProperty("service_types") java.util.Set<String> serviceTypes,
            Integer satisfaction,
            String memo,
            @JsonProperty("next_visit_cautions") String nextVisitCautions,
            @JsonProperty("photos") List<PublicPhotoResponse> photos
    ) {

        public static PublicRecordResponse from(SharedRecordView record) {
            return new PublicRecordResponse(record.performedAt(), record.salonName(),
                    record.designerName(), record.serviceTypes(), record.satisfaction(),
                    record.memo(), record.nextVisitCautions(),
                    record.photos() == null ? null
                            : record.photos().stream().map(PublicPhotoResponse::from).toList());
        }
    }

    public record PublicPhotoResponse(
            @Schema(description = "BEFORE/AFTER/OTHER")
            @JsonProperty("image_type") String imageType,

            @Schema(description = "조회 시점에 짧게 만료되는 표시용 URL. 다운로드 전용 URL 은 없다")
            @JsonProperty("display_url") String displayUrl
    ) {

        public static PublicPhotoResponse from(SharedPhotoView photo) {
            return new PublicPhotoResponse(photo.imageType(), photo.displayUrl().toString());
        }
    }

    /** 후보 스타일 도메인(#51 계획의 P4-3)이 자리 잡으면 채워진다. */
    public record PublicSavedStyleResponse(
    ) {
    }
}
