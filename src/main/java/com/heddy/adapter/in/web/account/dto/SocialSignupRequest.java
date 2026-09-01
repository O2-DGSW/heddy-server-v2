package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.port.in.SocialSignupCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@Schema(description = "소셜 회원가입 요청")
public record SocialSignupRequest(
        @NotNull
        @Schema(description = "소셜 인증 제공자",
                allowableValues = {"GOOGLE", "KAKAO", "APPLE"})
        AuthProvider provider,
        @NotBlank @Size(max = 4096)
        @Schema(description = "소셜 제공자에게 받은 인증 토큰 원문. 최대 4096자. 서버가 "
                + "제공자에 대조해 검증하며, 유효하지 않으면 401 AUTH_SOCIAL_TOKEN_INVALID. "
                + "이미 연결된 소셜 계정이면 409 AUTH_SOCIAL_ACCOUNT_ALREADY_LINKED",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("provider_token") String providerToken,

        @NotBlank @Size(max = 30)
        @Schema(description = "표시 이름. 최대 30자", requiredMode = Schema.RequiredMode.REQUIRED)
        String nickname,

        @Pattern(regexp = "^01[016789]\\d{7,8}$")
        @Schema(description = "휴대폰 번호. 선택 입력이며 하이픈 없이 숫자만",
                example = "01012345678")
        @JsonProperty("phone_number") String phoneNumber,

        @NotNull @Valid
        @Schema(description = "약관 동의. 5개 항목을 모두 보내야 한다",
                requiredMode = Schema.RequiredMode.REQUIRED)
        AgreementsRequest agreements
) {
    public SocialSignupCommand toCommand(String policyVersion) {
        return new SocialSignupCommand(
                provider, providerToken, nickname, phoneNumber,
                agreements.toDecisions(policyVersion));
    }
}
