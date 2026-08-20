package com.heddy.application.account.service;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.SmsVerificationPurpose;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.port.in.ResetPasswordCommand;
import com.heddy.domain.account.port.in.ResetPasswordUseCase;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.PasswordEncoderPort;
import com.heddy.domain.account.port.out.SmsVerificationStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResetPasswordService implements ResetPasswordUseCase {

    private final SmsVerificationStorePort smsVerificationStorePort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    @Override
    @Transactional
    public void reset(ResetPasswordCommand command) {
        if (!smsVerificationStorePort.isVerified(
                command.phoneNumber(), SmsVerificationPurpose.PASSWORD_RESET)) {
            throw new AccountException(AccountError.PHONE_NOT_VERIFIED);
        }
        Account account = accountRepositoryPort.findByPhoneNumber(command.phoneNumber())
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND));
        accountRepositoryPort.updatePassword(account.id(), passwordEncoderPort.encode(command.newPassword()));
        smsVerificationStorePort.deleteVerified(
                command.phoneNumber(), SmsVerificationPurpose.PASSWORD_RESET);
    }
}
