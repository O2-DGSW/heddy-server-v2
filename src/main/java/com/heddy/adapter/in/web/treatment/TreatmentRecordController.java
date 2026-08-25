package com.heddy.adapter.in.web.treatment;

import com.heddy.adapter.in.web.treatment.dto.CreateTreatmentRecordRequest;
import com.heddy.adapter.in.web.treatment.dto.TreatmentRecordResponse;
import com.heddy.adapter.in.web.treatment.dto.TreatmentRecordSummaryResponse;
import com.heddy.adapter.in.web.treatment.dto.UpdateTreatmentRecordRequest;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.port.in.CreateTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.DeleteTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.GetTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.ListTreatmentRecordsUseCase;
import com.heddy.domain.treatment.port.in.UpdateTreatmentRecordUseCase;
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

    @PostMapping("/treatment-records")
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "performedAt,desc 또는 performedAt,asc")
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
    @Operation(summary = "시술기록 단건 조회",
            description = "남의 기록은 존재 여부를 드러내지 않게 404 로 답한다. 사진 URL 은 저장값이 "
                    + "아니라 조회 시점에 짧은 만료의 Presigned GET 으로 발급된다.")
    public ApiResponse<TreatmentRecordResponse> get(
            @AuthenticationPrincipal UUID userId,
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
    @Operation(summary = "시술기록 부분 수정",
            description = "전달한 필드만 수정합니다. nullable 필드에 null을 보내면 값을 삭제합니다.")
    public ApiResponse<TreatmentRecordResponse> update(
            @AuthenticationPrincipal UUID userId,
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
    @Operation(summary = "시술기록 삭제",
            description = "기록과 사진 연결을 삭제하고 연결 파일은 비동기 회수 대상 상태로 전이합니다.")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID recordId
    ) {
        deleteTreatmentRecordUseCase.delete(
                new DeleteTreatmentRecordUseCase.Command(userId, recordId));
        return ResponseEntity.noContent().build();
    }
}
