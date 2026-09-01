package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.port.in.ResetPasswordCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 재설정 요청. 같은 번호·용도(PASSWORD_RESET)로 SMS 인증을 "
        + "먼저 통과해야 하며, 인증 없이 호출하면 422 AUTH_PHONE_NOT_VERIFIED 다")
public record ResetPasswordRequest(
        @NotBlank @Pattern(regexp = "^01[016789]\\d{7,8}$")
        @Schema(description = "인증을 마친 휴대폰 번호. 하이픈 없이 숫자만",
                example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("phone_number") String phoneNumber,

        @NotBlank @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*?_~])[a-zA-Z0-9!@#$%^&*?_~]+$")
        @Schema(description = "새 비밀번호. 8~100자이며 영문·숫자·특수문자를 각각 하나 이상 "
                + "포함해야 하고, 쓸 수 있는 문자는 영문·숫자와 특수문자 "
                + "! @ # $ % ^ & * ? _ ~ 뿐이다. 가입 때보다 규칙이 엄격하다 — 가입은 "
                + "특수문자를 요구하지 않으므로, 가입 때 쓰던 비밀번호가 여기서는 거부될 수 있다",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("new_password") String newPassword
) {
    public ResetPasswordCommand toCommand() {
        return new ResetPasswordCommand(phoneNumber, newPassword);
    }
}
