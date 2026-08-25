package com.heddy.adapter.in.web.sharing.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.heddy.domain.sharing.model.ShareFieldType;
import com.heddy.domain.sharing.port.in.UpdateShareUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 공유 수정 요청. 전달한 필드만 수정한다. 공유에는 "삭제해서 빌 수 있는" 필드가 없으므로
 * 시술기록과 달리 명시적 null 도 미전달과 같게 본다 — 값 교체는 반드시 실제 값을 담아 보낸다.
 * 대상(기록·후보)은 스펙 11.4 의 수정 범위가 아니라 요청에 있어도 무시한다.
 */
@Schema(description = "전달한 필드만 수정합니다")
public class UpdateShareRequest {

    private Set<ShareFieldType> fields;
    private Instant expiresAt;

    private boolean fieldsPresent;
    private boolean expiresAtPresent;

    public Set<ShareFieldType> getFields() {
        return fields;
    }

    @Schema(description = "노출 항목 교체. 1개 이상이어야 한다", allowableValues = {"PHOTOS",
            "TREATMENT_DETAILS", "SATISFACTION", "CAUTIONS", "MEMO", "SAVED_STYLES"})
    @JsonSetter("fields")
    void setFields(Set<ShareFieldType> fields) {
        this.fields = fields;
        fieldsPresent = true;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Schema(description = "만료 시각 교체. 현재보다 미래여야 한다", type = "string", format = "date-time")
    @JsonSetter("expires_at")
    void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        expiresAtPresent = true;
    }

    public UpdateShareUseCase.Command toCommand(UUID userId, UUID shareId) {
        return new UpdateShareUseCase.Command(userId, shareId,
                patch(fieldsPresent, fields),
                patch(expiresAtPresent, expiresAt));
    }

    private <T> UpdateShareUseCase.Patch<T> patch(boolean present, T value) {
        // 명시적 null 은 교체할 값이 아니라 미전달과 같다. 빈 배열(fields:[])은 남아 있어
        // 도메인의 SHARE_EMPTY_SELECTION 으로 거부된다.
        return present && value != null
                ? UpdateShareUseCase.Patch.present(value)
                : UpdateShareUseCase.Patch.absent();
    }
}
