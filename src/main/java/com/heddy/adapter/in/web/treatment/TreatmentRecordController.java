package com.heddy.adapter.in.web.treatment;

import com.heddy.adapter.in.web.treatment.dto.CreateTreatmentRecordRequest;
import com.heddy.adapter.in.web.treatment.dto.TreatmentRecordResponse;
import com.heddy.domain.treatment.port.in.CreateTreatmentRecordUseCase;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "시술기록", description = "시술기록 등록")
@SecurityRequirement(name = "bearerAuth")
public class TreatmentRecordController {

    private final CreateTreatmentRecordUseCase createTreatmentRecordUseCase;

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
}
