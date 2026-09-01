package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.heddy.domain.account.port.in.UpdateMyProfileCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "내 프로필 부분 수정 요청. 보낸 필드만 바뀌고 생략한 필드는 그대로 "
        + "남는다. 생략과 null 을 구분하기 위해 record 가 아니라 setter 로 받는다 — "
        + "선호 디자이너·주의사항은 null 을 보내 지울 수 있지만 닉네임·전화번호는 지울 수 없다")
public class UpdateMyProfileRequest {

    @Size(max = 30)
    @Schema(description = "표시 이름. 최대 30자. 지울 수 없는 값이라 null 이나 공백을 "
            + "보내면 422 VALIDATION_FAILED 다")
    private String nickname;

    @Pattern(regexp = "^01[016789]\\d{7,8}$")
    @Schema(description = "휴대폰 번호. 하이픈 없이 숫자만. 지울 수 없는 값이라 null 을 "
            + "보내면 422 VALIDATION_FAILED 다. 이미 다른 계정이 쓰는 번호면 "
            + "409 AUTH_PHONE_ALREADY_EXISTS", example = "01012345678")
    private String phone;

    @Size(max = 30)
    @Schema(description = "선호 디자이너 이름. 최대 30자. null 을 보내면 지워진다")
    private String preferredDesigner;

    @Schema(description = "모발 관련 주의사항 메모. null 을 보내면 지워진다")
    private String hairCautions;

    private boolean nicknamePresent;
    private boolean phonePresent;
    private boolean preferredDesignerPresent;
    private boolean hairCautionsPresent;

    @JsonSetter("nickname")
    public void setNickname(String nickname) {
        this.nickname = nickname;
        this.nicknamePresent = true;
    }

    @JsonSetter("phone")
    public void setPhone(String phone) {
        this.phone = phone;
        this.phonePresent = true;
    }

    @JsonSetter("preferred_designer")
    public void setPreferredDesigner(String preferredDesigner) {
        this.preferredDesigner = preferredDesigner;
        this.preferredDesignerPresent = true;
    }

    @JsonSetter("hair_cautions")
    public void setHairCautions(String hairCautions) {
        this.hairCautions = hairCautions;
        this.hairCautionsPresent = true;
    }

    public UpdateMyProfileCommand toCommand(UUID userId) {
        return new UpdateMyProfileCommand(userId, nicknamePresent, nickname,
                phonePresent, phone, preferredDesignerPresent, preferredDesigner,
                hairCautionsPresent, hairCautions);
    }
}
