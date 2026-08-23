package com.heddy.adapter.in.web.account.dto;

import com.heddy.domain.account.model.ConsentStatus;

import java.util.List;

public record ConsentsResponse(List<ConsentStatusResponse> items) {
    public static ConsentsResponse from(List<ConsentStatus> consents) {
        return new ConsentsResponse(
                consents.stream().map(ConsentStatusResponse::from).toList());
    }
}
