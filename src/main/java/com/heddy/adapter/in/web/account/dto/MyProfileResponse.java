package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.port.in.MyProfileResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "내 프로필 조회·수정 응답")
public record MyProfileResponse(
        @Schema(description = "계정 식별자")
        @JsonProperty("user_id") UUID userId,

        @Schema(description = "계정 이메일. 소문자로 정규화된 값이다. 소셜 가입 계정은 비어 있을 수 있다")
        String email,

        @Schema(description = "표시 이름")
        String nickname,

        @Schema(description = "휴대폰 번호. 하이픈 없는 숫자이며, 가입 때 넣지 않았으면 비어 있다")
        String phone,

        @Schema(description = "선호 디자이너 이름. 입력하지 않았거나 지웠으면 비어 있다")
        @JsonProperty("preferred_designer") String preferredDesigner,

        @Schema(description = "모발 관련 주의사항 메모. 입력하지 않았거나 지웠으면 비어 있다")
        @JsonProperty("hair_cautions") String hairCautions,

        @Schema(description = "계정 상태",
                allowableValues = {"ACTIVE", "LOCKED", "DELETION_PENDING", "DELETED"})
        AccountStatus status,

        @Schema(description = "계정 생성 시각")
        @JsonProperty("created_at") Instant createdAt,

        @Schema(description = "프로필이 마지막으로 바뀐 시각")
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static MyProfileResponse from(MyProfileResult result) {
        return new MyProfileResponse(result.userId(), result.email(), result.nickname(),
                result.phone(), result.preferredDesigner(), result.hairCautions(), result.status(),
                result.createdAt(), result.updatedAt());
    }
}
