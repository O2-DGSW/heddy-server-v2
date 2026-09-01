package com.heddy.global.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "실패 응답의 공통 형태. 성공 응답과 같은 봉투를 쓰되 data 대신 error 가 "
        + "채워진다")
public record ApiErrorResponse(
        @Schema(description = "실패 상세")
        ErrorBody error,

        @Schema(description = "이 요청의 추적 식별자. 문의할 때 이 값을 함께 알려주면 서버 "
                + "로그에서 해당 요청을 찾을 수 있다")
        @JsonProperty("request_id") String requestId
) {
    @Schema(name = "ApiError")
    public record ErrorBody(
            @Schema(description = "실패 코드. 클라이언트는 HTTP 상태코드가 아니라 이 값으로 "
                    + "분기한다 — 같은 상태코드에 여러 코드가 올 수 있다",
                    example = "RESOURCE_NOT_FOUND")
            String code,

            @Schema(description = "사람이 읽을 안내 문구. 문구는 예고 없이 바뀔 수 있으므로 "
                    + "이 값으로 분기하지 않는다")
            String message,

            @Schema(description = "필드별 검증 실패 목록. VALIDATION_FAILED 일 때만 채워지고 "
                    + "그 밖에는 빈 배열이다")
            @JsonProperty("field_errors") List<FieldError> fieldErrors
    ) {
    }

    @Schema(name = "ApiFieldError")
    public record FieldError(
            @Schema(description = "검증에 실패한 필드 이름", example = "email") String field,
            @Schema(description = "실패 사유") String reason
    ) {
    }

    public static ApiErrorResponse of(String code, String message, String requestId) {
        return new ApiErrorResponse(new ErrorBody(code, message, List.of()), requestId);
    }

    public static ApiErrorResponse validation(String requestId, List<FieldError> errors) {
        return new ApiErrorResponse(
                new ErrorBody(ErrorCode.VALIDATION_FAILED.code(),
                        ErrorCode.VALIDATION_FAILED.message(), errors),
                requestId);
    }
}
