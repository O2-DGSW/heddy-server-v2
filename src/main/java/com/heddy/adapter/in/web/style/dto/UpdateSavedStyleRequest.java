package com.heddy.adapter.in.web.style.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.heddy.domain.style.port.in.SavedStyleUseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "저장 후보 메모 부분 수정 요청. null을 보내면 메모를 삭제합니다.")
public class UpdateSavedStyleRequest {

    @Size(max = 500)
    private String memo;
    private boolean memoPresent;

    @JsonSetter("memo")
    public void setMemo(String memo) {
        this.memo = memo;
        memoPresent = true;
    }

    public SavedStyleUseCase.UpdateMemoCommand toCommand(UUID userId, UUID savedStyleId) {
        return new SavedStyleUseCase.UpdateMemoCommand(
                userId, savedStyleId, memoPresent, memo);
    }
}
