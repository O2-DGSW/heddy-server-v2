package com.heddy.adapter.in.web.account.dto;

import com.heddy.domain.account.port.in.EmailAvailabilityResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 중복 확인 응답")
public record EmailAvailabilityResponse(
        @Schema(description = "확인한 이메일. 소문자로 정규화된 값이다") String email,
        @Schema(description = "가입에 쓸 수 있으면 true. 확인 시점 기준이라 그 사이 다른 "
                + "사람이 가입하면 가입 요청은 409 로 실패할 수 있다") boolean available
) {
    public static EmailAvailabilityResponse from(EmailAvailabilityResult result) {
        return new EmailAvailabilityResponse(result.email(), result.available());
    }
}
