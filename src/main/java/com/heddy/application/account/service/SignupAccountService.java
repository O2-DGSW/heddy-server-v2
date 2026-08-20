package com.heddy.application.account.service;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.SmsVerificationPurpose;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.port.in.SignupAccountCommand;
import com.heddy.domain.account.port.in.SignupAccountUseCase;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.PasswordEncoderPort;
import com.heddy.domain.account.port.out.SmsVerificationStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupAccountService implements SignupAccountUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final SmsVerificationStorePort smsVerificationStorePort;
    private final PasswordEncoderPort passwordEncoderPort;

    @Override
    @Transactional
    public void signup(SignupAccountCommand command) {
        if (accountRepositoryPort.existsByLoginId(command.loginId())) {
            throw new AccountException(AccountError.LOGIN_ID_DUPLICATED);
        }
        if (accountRepositoryPort.existsByPhoneNumber(command.phoneNumber())) {
            throw new AccountException(AccountError.PHONE_DUPLICATED);
        }
        if (!smsVerificationStorePort.isVerified(command.phoneNumber(), SmsVerificationPurpose.SIGNUP)) {
            throw new AccountException(AccountError.PHONE_NOT_VERIFIED);
        }

        accountRepositoryPort.save(Account.local(
                command.loginId(),
                passwordEncoderPort.encode(command.password()),
                command.name(),
                command.phoneNumber()));
        smsVerificationStorePort.deleteVerified(command.phoneNumber(), SmsVerificationPurpose.SIGNUP);
    }
}
