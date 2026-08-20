package com.heddy.adapter.in.web.account.dto;

import com.heddy.domain.account.port.in.EmailAvailabilityResult;

public record EmailAvailabilityResponse(String email, boolean available) {
    public static EmailAvailabilityResponse from(EmailAvailabilityResult result) {
        return new EmailAvailabilityResponse(result.email(), result.available());
    }
}
