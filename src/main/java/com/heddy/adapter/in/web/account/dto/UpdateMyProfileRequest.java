package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.heddy.domain.account.port.in.UpdateMyProfileCommand;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class UpdateMyProfileRequest {

    @Size(max = 30)
    private String nickname;
    @Pattern(regexp = "^01[016789]\\d{7,8}$")
    private String phone;
    @Size(max = 30)
    private String preferredDesigner;
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
