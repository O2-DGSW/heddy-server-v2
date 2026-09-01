package com.heddy.adapter.in.web.account.dto;

import com.heddy.domain.account.model.ConsentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "동의 현황 조회 응답")
public record ConsentsResponse(
        @Schema(description = "동의 유형별 현재 상태. 유형마다 가장 최근 이력 한 건이다")
        List<ConsentStatusResponse> items
) {
    public static ConsentsResponse from(List<ConsentStatus> consents) {
        return new ConsentsResponse(
                consents.stream().map(ConsentStatusResponse::from).toList());
    }
}
