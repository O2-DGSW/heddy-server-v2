package com.heddy.adapter.in.web.treatment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.port.in.CreateTreatmentRecordUseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 등록 요청 본문. 여기서는 형식 검증(필수 여부)만 하고 나머지 규칙은 도메인이 검증한다 —
 * 같은 규칙을 두 곳에서 검사하면 어느 하나는 반드시 뒤처진다.
 */
public record CreateTreatmentRecordRequest(
        @NotEmpty
        @Schema(description = "시술 종류. CUT PERM COLOR BLEACH CLINIC STYLING OTHER 중 1개 이상")
        @JsonProperty("service_types") Set<@NotNull ServiceType> serviceTypes,

        @NotNull
        @Schema(description = "시술일시(ISO-8601). 미래일 수 없다")
        @JsonProperty("performed_at") Instant performedAt,

        @Schema(description = "미용실 이름. 선택 입력, 최대 50자")
        @JsonProperty("salon_name") String salonName,

        @Schema(description = "디자이너 이름. 선택 입력, 최대 30자")
        @JsonProperty("designer_name") String designerName,

        @Schema(description = "만족도. 선택 입력, 1~5")
        @JsonProperty("satisfaction") Integer satisfaction,

        @Schema(description = "소요 시간(분). 선택 입력, 0 이상", example = "90")
        @JsonProperty("duration_minutes") Integer durationMinutes,

        @Schema(description = "시술 내용. 선택 입력, 최대 255자. 메모와 별개로 무엇을 했는지 "
                + "적는다", example = "애쉬브라운 전체 염색")
        @JsonProperty("treatment_content") String treatmentContent,

        @Schema(description = "가격 금액. price_currency 와 함께 넣거나 함께 뺀다")
        @JsonProperty("price_amount") Long priceAmount,

        @Schema(description = "통화 코드(3자). price_amount 와 함께 넣거나 함께 뺀다")
        @JsonProperty("price_currency") String priceCurrency,

        @Schema(description = "연결할 예약 식별자. 선택 입력")
        @JsonProperty("appointment_id") UUID appointmentId,

        @Schema(description = "개인 메모. 선택 입력")
        @JsonProperty("memo") String memo,

        @Schema(description = "다음 방문 시 주의사항. 선택 입력")
        @JsonProperty("next_visit_cautions") String nextVisitCautions,

        @Valid
        @Schema(description = "첨부 사진. READY 인 요청자 소유 파일만 가리키며 최대 10장")
        @JsonProperty("photos") List<@NotNull @Valid PhotoRequest> photos
) {

    public record PhotoRequest(
            @NotNull
            @Schema(description = "업로드를 마친 파일의 식별자")
            @JsonProperty("file_id") UUID fileId,

            @NotNull
            @Schema(description = "촬영 시점 구분. BEFORE AFTER OTHER")
            @JsonProperty("image_type") ImageType imageType,

            @Schema(description = "표시 순서. 생략하면 요청 배열 순서")
            @JsonProperty("sort_order") Integer sortOrder
    ) {
        public PhotoRequest(UUID fileId, ImageType imageType) {
            this(fileId, imageType, null);
        }
    }

    public CreateTreatmentRecordUseCase.Command toCommand(UUID userId) {
        return new CreateTreatmentRecordUseCase.Command(userId, serviceTypes, salonName, designerName,
                performedAt, satisfaction, priceAmount, priceCurrency, appointmentId,
                memo, nextVisitCautions, durationMinutes, treatmentContent,
                photos == null ? List.of() : java.util.stream.IntStream.range(0, photos.size())
                        .mapToObj(index -> {
                            PhotoRequest photo = photos.get(index);
                            return new CreateTreatmentRecordUseCase.Command.Photo(
                                    photo.fileId(), photo.imageType(),
                                    photo.sortOrder() == null ? index : photo.sortOrder());
                        }).toList());
    }
}
