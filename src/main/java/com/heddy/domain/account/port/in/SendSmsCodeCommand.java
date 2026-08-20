package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.SmsVerificationPurpose;

public record SendSmsCodeCommand(
        String phoneNumber,
        String carrier,
        SmsVerificationPurpose purpose
) {
}
