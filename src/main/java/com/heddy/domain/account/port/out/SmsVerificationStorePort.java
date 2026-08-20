package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.SmsVerification;
import com.heddy.domain.account.model.SmsVerificationPurpose;

import java.util.Optional;

public interface SmsVerificationStorePort {
    void save(String phoneNumber, SmsVerification verification);
    Optional<SmsVerification> find(String phoneNumber);
    void delete(String phoneNumber);
    void startCooldown(String phoneNumber);
    boolean hasCooldown(String phoneNumber);
    void markVerified(String phoneNumber, SmsVerificationPurpose purpose);
    boolean isVerified(String phoneNumber, SmsVerificationPurpose purpose);
    void deleteVerified(String phoneNumber, SmsVerificationPurpose purpose);
}
