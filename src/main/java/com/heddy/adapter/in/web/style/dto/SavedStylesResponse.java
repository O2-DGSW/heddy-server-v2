package com.heddy.adapter.in.web.style.dto;

import com.heddy.domain.style.port.in.SavedStyleUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "저장한 후보 스타일 목록 응답")
public record SavedStylesResponse(
        @Schema(description = "최신 저장순. 저장한 후보가 없으면 빈 배열이다")
        List<SavedStyleResponse> items
) {
    public static SavedStylesResponse from(List<SavedStyleUseCase.Item> items) {
        return new SavedStylesResponse(items.stream().map(SavedStyleResponse::from).toList());
    }
}
