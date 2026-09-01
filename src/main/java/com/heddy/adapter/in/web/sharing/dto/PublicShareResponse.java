package com.heddy.adapter.in.web.sharing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.sharing.model.SharedContentView.SharedPhotoView;
import com.heddy.domain.sharing.model.SharedContentView.SharedRecordView;
import com.heddy.domain.sharing.model.SharedContentView.SharedSavedStyleView;
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

        @Schema(description = "공유된 저장 후보 스타일. 항목을 선택하지 않았으면 키를 제외한다")
        @JsonProperty("saved_styles") List<PublicSavedStyleResponse> savedStyles
) {

    public static PublicShareResponse from(GetPublicShareUseCase.Result result) {
        return new PublicShareResponse(
                new ShareHeader(result.expiresAt(), result.content().ownerDisplayName()),
                result.content().records().stream().map(PublicRecordResponse::from).toList(),
                result.content().savedStyles() == null ? null
                        : result.content().savedStyles().stream()
                                .map(PublicSavedStyleResponse::from)
                                .toList());
    }

    public record ShareHeader(
            @Schema(description = "링크 만료 시각. 이 시각이 지나면 SHARE_EXPIRED 로 답한다")
            @JsonProperty("expires_at") Instant expiresAt,

            @Schema(description = "공유한 사람의 표시 이름. user_id 는 노출하지 않는다")
            @JsonProperty("owner_display_name") String ownerDisplayName
    ) {
    }

    /** 미선택 항목은 값이 아니라 키 자체가 빠진다. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PublicRecordResponse(
            @Schema(description = "시술일시. TREATMENT_DETAILS 를 선택하지 않았으면 키가 빠진다")
            @JsonProperty("performed_at") Instant performedAt,

            @Schema(description = "미용실 이름. TREATMENT_DETAILS 를 선택하지 않았으면 키가 빠진다")
            @JsonProperty("salon_name") String salonName,

            @Schema(description = "디자이너 이름. TREATMENT_DETAILS 를 선택하지 않았으면 키가 빠진다")
            @JsonProperty("designer_name") String designerName,

            @Schema(description = "시술 종류 목록. TREATMENT_DETAILS 를 선택하지 않았으면 키가 빠진다")
            @JsonProperty("service_types") java.util.Set<String> serviceTypes,

            @Schema(description = "만족도(1~5). SATISFACTION 을 선택하지 않았으면 키가 빠진다")
            Integer satisfaction,

            @Schema(description = "메모. MEMO 를 선택하지 않았으면 키가 빠진다")
            String memo,

            @Schema(description = "다음 방문 주의사항. CAUTIONS 를 선택하지 않았으면 키가 빠진다")
            @JsonProperty("next_visit_cautions") String nextVisitCautions,

            @Schema(description = "사진 목록. PHOTOS 를 선택하지 않았으면 키가 빠진다")
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

    public record PublicSavedStyleResponse(
            @Schema(description = "저장 당시의 스타일 이름")
            @JsonProperty("style_name") String styleName,

            @Schema(description = "스타일 이미지 URL")
            @JsonProperty("image_url") String imageUrl,

            @Schema(description = "이 스타일을 추천받은 이유")
            String reason
    ) {

        public static PublicSavedStyleResponse from(SharedSavedStyleView savedStyle) {
            return new PublicSavedStyleResponse(
                    savedStyle.styleName(), savedStyle.imageUrl(), savedStyle.reason());
        }
    }
}
