package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.port.in.EmailSignupCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@Schema(description = "이메일 회원가입 요청")
public record EmailSignupRequest(
        @NotBlank @Email @Size(max = 255)
        @Schema(description = "계정 식별자. 최대 255자이며 서버가 소문자로 변환해 저장한다 — "
                + "대소문자만 다른 주소는 같은 계정으로 취급된다. 이미 쓰이는 주소면 "
                + "409 AUTH_EMAIL_ALREADY_EXISTS",
                example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @NotBlank @Size(min = 8, max = 100)
        @Schema(description = "8~100자, 영문과 숫자를 각각 하나 이상 포함해야 한다. 어기면 "
                + "422 AUTH_WEAK_PASSWORD. 비밀번호 재설정은 여기에 더해 특수문자를 요구하므로 "
                + "재설정으로 되돌릴 수 있는 값을 쓰려면 특수문자도 넣는 편이 안전하다",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String password,

        @NotBlank @Size(max = 30)
        @Schema(description = "표시 이름. 최대 30자", requiredMode = Schema.RequiredMode.REQUIRED)
        String nickname,

        @Pattern(regexp = "^01[016789]\\d{7,8}$")
        @Schema(description = "휴대폰 번호. 선택 입력이며 하이픈 없이 숫자만 넣는다"
                + "(010·011·016·017·018·019 로 시작하는 10~11자리)",
                example = "01012345678")
        @JsonProperty("phone_number") String phoneNumber,

        @NotNull @Valid
        @Schema(description = "약관 동의. 5개 항목을 모두 보내야 한다",
                requiredMode = Schema.RequiredMode.REQUIRED)
        AgreementsRequest agreements
) {
    public EmailSignupCommand toCommand(String policyVersion) {
        return new EmailSignupCommand(
                email.toLowerCase(), password, nickname, phoneNumber,
                agreements.toDecisions(policyVersion));
    }
}
