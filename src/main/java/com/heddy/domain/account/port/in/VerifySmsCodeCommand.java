package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.SmsVerificationPurpose;

public record VerifySmsCodeCommand(
        String phoneNumber,
        String code,
        SmsVerificationPurpose purpose
) {
}
