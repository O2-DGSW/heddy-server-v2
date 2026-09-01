package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.SmsVerificationPurpose;
import com.heddy.domain.account.port.in.SendSmsCodeCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "SMS 인증번호 발송 요청. 직전 발송으로부터 일정 시간이 지나지 않으면 "
        + "429 AUTH_SMS_SEND_TOO_SOON, 발송 자체가 실패하면 503 AUTH_SMS_SEND_FAILED 다")
public record SendSmsCodeRequest(
        @NotBlank @Pattern(regexp = "^01[016789]\\d{7,8}$")
        @Schema(description = "인증번호를 받을 휴대폰 번호. 하이픈 없이 숫자만",
                example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("phone_number") String phoneNumber,

        @NotBlank @Size(max = 20)
        @Schema(description = "통신사. 최대 20자", requiredMode = Schema.RequiredMode.REQUIRED)
        String carrier,

        @NotNull
        @Schema(description = "인증 용도. 검증 요청의 purpose 와 같아야 하며, 다르면 발송된 "
                + "코드로 검증되지 않는다",
                allowableValues = {"SIGNUP", "PASSWORD_RESET", "PHONE_CHANGE"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        SmsVerificationPurpose purpose
) {
    public SendSmsCodeCommand toCommand() {
        return new SendSmsCodeCommand(phoneNumber, carrier, purpose);
    }
}
