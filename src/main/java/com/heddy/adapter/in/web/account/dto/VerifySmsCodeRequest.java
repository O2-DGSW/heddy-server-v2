package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.SmsVerificationPurpose;
import com.heddy.domain.account.port.in.VerifySmsCodeCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "SMS 인증번호 검증 요청. 코드가 틀리면 422 AUTH_SMS_CODE_INVALID, "
        + "시도 횟수를 넘기면 423 AUTH_SMS_CODE_MAX_ATTEMPTS, 발송 기록이 없거나 만료됐으면 "
        + "404 AUTH_SMS_CODE_NOT_FOUND 다")
public record VerifySmsCodeRequest(
        @NotBlank @Pattern(regexp = "^01[016789]\\d{7,8}$")
        @Schema(description = "인증번호를 받은 휴대폰 번호. 발송 요청과 같은 번호여야 한다",
                example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("phone_number") String phoneNumber,

        @NotBlank @Size(min = 6, max = 6)
        @Schema(description = "문자로 받은 인증번호. 정확히 6자리",
                example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @NotNull
        @Schema(description = "인증 용도. 발송 요청의 purpose 와 같아야 한다",
                allowableValues = {"SIGNUP", "PASSWORD_RESET", "PHONE_CHANGE"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        SmsVerificationPurpose purpose
) {
    public VerifySmsCodeCommand toCommand() {
        return new VerifySmsCodeCommand(phoneNumber, code, purpose);
    }
}
