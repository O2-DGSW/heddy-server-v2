package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.SmsVerificationPurpose;
import com.heddy.domain.account.port.out.SmsVerificationStorePort;
import com.heddy.domain.account.port.out.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignupPhoneVerificationService {

    private final UserProfileRepositoryPort userProfileRepositoryPort;
    private final SmsVerificationStorePort smsVerificationStorePort;

    public void validate(String phone) {
        if (phone == null) {
            return;
        }
        if (userProfileRepositoryPort.existsByPhone(phone)) {
            throw new AccountException(AccountError.PHONE_ALREADY_EXISTS);
        }
        if (!smsVerificationStorePort.isVerified(phone, SmsVerificationPurpose.SIGNUP)) {
            throw new AccountException(AccountError.PHONE_NOT_VERIFIED);
        }
    }

    public void consume(String phone) {
        if (phone != null) {
            smsVerificationStorePort.deleteVerified(phone, SmsVerificationPurpose.SIGNUP);
        }
    }
}
