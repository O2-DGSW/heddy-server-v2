package com.heddy.adapter.in.web.file.controller;

import com.heddy.adapter.in.web.file.dto.PresignUploadRequest;
import com.heddy.adapter.in.web.file.dto.PresignUploadResponse;
import com.heddy.adapter.in.web.file.dto.CompleteUploadResponse;
import com.heddy.domain.file.port.in.CompleteUploadCommand;
import com.heddy.domain.file.port.in.CompleteUploadUseCase;
import com.heddy.domain.file.port.in.PresignUploadUseCase;
import com.heddy.global.filter.RequestIdFilter;
import com.heddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "업로드", description = "S3 Presigned 업로드 세션 발급·완료")
@SecurityRequirement(name = "bearerAuth")
public class UploadController {

    private final PresignUploadUseCase presignUploadUseCase;
    private final CompleteUploadUseCase completeUploadUseCase;

    @PostMapping("/uploads/presign")
    @Operation(summary = "업로드 세션 발급",
            description = "purpose 별 최대 크기와 허용 Content-Type 을 검증해 PENDING 업로드 세션을 만들고 "
                    + "Presigned PUT URL 을 돌려준다. 클라이언트는 이 URL 로 스토리지에 직접 올린다.")
    public ApiResponse<PresignUploadResponse> presign(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PresignUploadRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                PresignUploadResponse.from(presignUploadUseCase.presign(request.toCommand(userId))),
                RequestIdFilter.get(servletRequest));
    }

    @PostMapping("/uploads/{uploadId}/complete")
    @Operation(summary = "업로드 완료",
            description = "스토리지 HEAD 로 객체 존재·크기·Content-Type 을 대조한 뒤 PENDING 을 READY 로 "
                    + "전이한다. 이미 READY 인 세션에 대한 재요청은 저장된 결과를 다시 돌려준다.")
    public ApiResponse<CompleteUploadResponse> complete(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID uploadId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                CompleteUploadResponse.from(completeUploadUseCase.complete(
                        new CompleteUploadCommand(userId, uploadId))),
                RequestIdFilter.get(servletRequest));
    }
}
