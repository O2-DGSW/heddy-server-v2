package com.heddy.adapter.in.web.treatment;

import com.heddy.adapter.in.web.treatment.dto.CreateTreatmentRecordRequest;
import com.heddy.adapter.in.web.treatment.dto.AddTreatmentPhotoRequest;
import com.heddy.adapter.in.web.treatment.dto.PhotoComparisonResponse;
import com.heddy.adapter.in.web.treatment.dto.TreatmentRecordResponse;
import com.heddy.adapter.in.web.treatment.dto.TreatmentPhotoResponse;
import com.heddy.adapter.in.web.treatment.dto.TreatmentRecordSummaryResponse;
import com.heddy.adapter.in.web.treatment.dto.UpdateTreatmentRecordRequest;
import com.heddy.adapter.in.web.treatment.dto.UpdateTreatmentPhotoRequest;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.port.in.CreateTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.DeleteTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.GetTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.GetPhotoComparisonUseCase;
import com.heddy.domain.treatment.port.in.ListTreatmentRecordsUseCase;
import com.heddy.domain.treatment.port.in.ManageTreatmentPhotosUseCase;
import com.heddy.domain.treatment.port.in.UpdateTreatmentRecordUseCase;
import com.heddy.global.docs.ApiDocs;
import com.heddy.global.filter.RequestIdFilter;
import com.heddy.global.response.ApiResponse;
import com.heddy.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "시술기록", description = "시술기록 등록·목록·단건 조회")
@SecurityRequirement(name = "bearerAuth")
public class TreatmentRecordController {

    private final CreateTreatmentRecordUseCase createTreatmentRecordUseCase;
    private final GetTreatmentRecordUseCase getTreatmentRecordUseCase;
    private final ListTreatmentRecordsUseCase listTreatmentRecordsUseCase;
    private final UpdateTreatmentRecordUseCase updateTreatmentRecordUseCase;
    private final DeleteTreatmentRecordUseCase deleteTreatmentRecordUseCase;
    private final ManageTreatmentPhotosUseCase manageTreatmentPhotosUseCase;
    private final GetPhotoComparisonUseCase getPhotoComparisonUseCase;

    @PostMapping("/treatment-records")
    @ApiDocs.Created
    @ApiDocs.Authenticated
    @ApiDocs.Validated
    @ApiDocs.PhotoAttachment
    @Operation(summary = "시술기록 등록",
            description = "시술 종류는 1개 이상, 시술일은 미래일 수 없다. 첨부 사진은 READY 인 "
                    + "요청자 소유 파일(file_id)만 가리킬 수 있다.")
    public ResponseEntity<ApiResponse<TreatmentRecordResponse>> create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateTreatmentRecordRequest request,
            HttpServletRequest servletRequest
    ) {
        // 새 리소스를 만드는 API 라 201 로 답한다.
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                TreatmentRecordResponse.core(
                        createTreatmentRecordUseCase.create(request.toCommand(userId))),
                RequestIdFilter.get(servletRequest)));
    }

    @GetMapping("/treatment-records")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @ApiDocs.ListQuery
    @Operation(summary = "시술기록 목록 조회",
            description = "내 기록만 시술 종류·디자이너·미용실·시술일 범위로 필터링하고 페이지로 조회합니다. "
                    + "기록이 없으면 200과 빈 items를 반환합니다.")
    public ApiResponse<PageResponse<TreatmentRecordSummaryResponse>> list(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "시술 종류")
            @RequestParam(name = "service_type", required = false) ServiceType serviceType,
            @Parameter(description = "담당 디자이너 이름과 정확히 일치")
            @RequestParam(name = "designer_name", required = false) String designerName,
            @Parameter(description = "미용실 이름과 정확히 일치")
            @RequestParam(name = "salon_name", required = false) String salonName,
            @Parameter(description = "시술일 조회 시작(포함), ISO 8601")
            @RequestParam(required = false) Instant from,
            @Parameter(description = "시술일 조회 종료(포함), ISO 8601")
            @RequestParam(required = false) Instant to,
            @Parameter(description = "0부터 시작하는 페이지 번호. 음수면 400 INVALID_REQUEST")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지 크기. 1~100 이며 벗어나면 400 INVALID_REQUEST")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 기준. performedAt,desc 또는 performedAt,asc 만 "
                    + "허용하며 그 밖의 값은 400 INVALID_REQUEST")
            @RequestParam(defaultValue = "performedAt,desc") String sort,
            HttpServletRequest servletRequest
    ) {
        ListTreatmentRecordsUseCase.Result result = listTreatmentRecordsUseCase.list(
                new ListTreatmentRecordsUseCase.Query(
                        userId, serviceType, designerName, salonName, from, to, page, size, sort));
        PageResponse<TreatmentRecordSummaryResponse> response = PageResponse.of(
                result.items().stream().map(TreatmentRecordSummaryResponse::from).toList(),
                result.page(), result.size(), result.totalElements());
        return ApiResponse.success(response, RequestIdFilter.get(servletRequest));
    }

    @GetMapping("/treatment-records/{recordId}")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @ApiDocs.OwnedResource
    @Operation(summary = "시술기록 단건 조회",
            description = "남의 기록은 존재 여부를 드러내지 않게 404 로 답한다. 사진 URL 은 저장값이 "
                    + "아니라 조회 시점에 짧은 만료의 Presigned GET 으로 발급된다.")
    public ApiResponse<TreatmentRecordResponse> get(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "시술기록 식별자. 남의 기록은 존재 여부를 드러내지 않게 "
                    + "없는 기록과 같은 404 로 답한다")
            @PathVariable UUID recordId,
            HttpServletRequest servletRequest
    ) {
        GetTreatmentRecordUseCase.Result result =
                getTreatmentRecordUseCase.get(new GetTreatmentRecordUseCase.Query(userId, recordId));
        return ApiResponse.success(
                TreatmentRecordResponse.withPhotos(result.record(), result.photoUrls()),
                RequestIdFilter.get(servletRequest));
    }

    @PatchMapping("/treatment-records/{recordId}")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @ApiDocs.Validated
    @ApiDocs.OwnedResource
    @Operation(summary = "시술기록 부분 수정",
            description = "전달한 필드만 수정합니다. nullable 필드에 null을 보내면 값을 삭제합니다.")
    public ApiResponse<TreatmentRecordResponse> update(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "시술기록 식별자. 남의 기록은 존재 여부를 드러내지 않게 "
                    + "없는 기록과 같은 404 로 답한다")
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateTreatmentRecordRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                TreatmentRecordResponse.core(
                        updateTreatmentRecordUseCase.update(request.toCommand(userId, recordId))),
                RequestIdFilter.get(servletRequest));
    }

    @DeleteMapping("/treatment-records/{recordId}")
    @ApiDocs.NoContent
    @ApiDocs.Authenticated
    @ApiDocs.OwnedResource
    @Operation(summary = "시술기록 삭제",
            description = "기록과 사진 연결을 삭제하고 연결 파일은 비동기 회수 대상 상태로 전이합니다.")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "시술기록 식별자. 남의 기록은 존재 여부를 드러내지 않게 "
                    + "없는 기록과 같은 404 로 답한다")
            @PathVariable UUID recordId
    ) {
        deleteTreatmentRecordUseCase.delete(
                new DeleteTreatmentRecordUseCase.Command(userId, recordId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/treatment-records/{recordId}/photos")
    @ApiDocs.Created
    @ApiDocs.Authenticated
    @ApiDocs.Validated
    @ApiDocs.OwnedResource
    @ApiDocs.PhotoAttachment
    @Operation(summary = "시술기록 사진 추가",
            description = "READY 상태인 요청자 소유 파일을 연결합니다. 기록당 최대 10장입니다.")
    public ResponseEntity<ApiResponse<TreatmentPhotoResponse>> addPhoto(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "시술기록 식별자. 남의 기록은 존재 여부를 드러내지 않게 "
                    + "없는 기록과 같은 404 로 답한다")
            @PathVariable UUID recordId,
            @Valid @RequestBody AddTreatmentPhotoRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                TreatmentPhotoResponse.from(
                        manageTreatmentPhotosUseCase.add(request.toCommand(userId, recordId))),
                RequestIdFilter.get(servletRequest)));
    }

    @PatchMapping("/treatment-records/{recordId}/photos/{photoId}")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @ApiDocs.Validated
    @ApiDocs.OwnedResource
    @ApiDocs.PhotoAttachment
    @Operation(summary = "시술기록 사진 수정",
            description = "가리키는 파일, 사진 유형, 표시 순서 중 전달한 값을 수정합니다. "
                    + "file_id 를 보내면 사진을 다른 파일로 교체하며 photo_id 는 그대로 둡니다 "
                    + "— 삭제 후 재등록과 달리 표시 순서와 이 사진을 참조하는 분석 결과가 끊기지 않습니다.")
    public ApiResponse<TreatmentPhotoResponse> updatePhoto(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "시술기록 식별자. 남의 기록은 존재 여부를 드러내지 않게 "
                    + "없는 기록과 같은 404 로 답한다")
            @PathVariable UUID recordId,
            @Parameter(description = "사진 식별자. 위 기록에 속한 사진이어야 하며, 아니면 404")
            @PathVariable UUID photoId,
            @Valid @RequestBody UpdateTreatmentPhotoRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                TreatmentPhotoResponse.from(manageTreatmentPhotosUseCase.update(
                        request.toCommand(userId, recordId, photoId))),
                RequestIdFilter.get(servletRequest));
    }

    @DeleteMapping("/treatment-records/{recordId}/photos/{photoId}")
    @ApiDocs.NoContent
    @ApiDocs.Authenticated
    @ApiDocs.OwnedResource
    @Operation(summary = "시술기록 사진 삭제",
            description = "사진 연결을 삭제하고 연결 파일을 비동기 회수 대상 상태로 전이합니다.")
    public ResponseEntity<Void> deletePhoto(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "시술기록 식별자. 남의 기록은 존재 여부를 드러내지 않게 "
                    + "없는 기록과 같은 404 로 답한다")
            @PathVariable UUID recordId,
            @Parameter(description = "사진 식별자. 위 기록에 속한 사진이어야 하며, 아니면 404")
            @PathVariable UUID photoId
    ) {
        manageTreatmentPhotosUseCase.delete(
                new ManageTreatmentPhotosUseCase.DeleteCommand(userId, recordId, photoId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/treatment-records/{recordId}/photo-comparison")
    @ApiDocs.Ok
    @ApiDocs.Authenticated
    @ApiDocs.OwnedResource
    @ApiDocs.PhotoComparison
    @Operation(summary = "시술 전후 사진 비교 조회",
            description = "BEFORE와 AFTER 사진이 모두 있어야 하며, 한쪽이라도 없으면 422를 반환합니다.")
    public ApiResponse<PhotoComparisonResponse> getPhotoComparison(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "시술기록 식별자. 남의 기록은 존재 여부를 드러내지 않게 "
                    + "없는 기록과 같은 404 로 답한다")
            @PathVariable UUID recordId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                PhotoComparisonResponse.from(getPhotoComparisonUseCase.getPhotoComparison(
                        new GetPhotoComparisonUseCase.Query(userId, recordId))),
                RequestIdFilter.get(servletRequest));
    }
}
