package com.heddy.adapter.in.web.treatment;

import com.heddy.adapter.in.web.treatment.dto.CreateTreatmentRecordRequest;
import com.heddy.adapter.in.web.treatment.dto.TreatmentRecordResponse;
import com.heddy.domain.treatment.port.in.CreateTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.GetTreatmentRecordUseCase;
import com.heddy.global.filter.RequestIdFilter;
import com.heddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "시술기록", description = "시술기록 등록·단건 조회")
@SecurityRequirement(name = "bearerAuth")
public class TreatmentRecordController {

    private final CreateTreatmentRecordUseCase createTreatmentRecordUseCase;
    private final GetTreatmentRecordUseCase getTreatmentRecordUseCase;

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
}
