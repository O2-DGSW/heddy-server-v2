package com.heddy.application.account.service;

import com.heddy.domain.account.model.SmsVerification;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.port.in.SendSmsCodeCommand;
import com.heddy.domain.account.port.in.SendSmsCodeUseCase;
import com.heddy.domain.account.port.in.VerifySmsCodeCommand;
import com.heddy.domain.account.port.in.VerifySmsCodeUseCase;
import com.heddy.domain.account.port.out.SmsSenderPort;
import com.heddy.domain.account.port.out.SmsVerificationStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SmsVerificationService implements SendSmsCodeUseCase, VerifySmsCodeUseCase {

    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SmsVerificationStorePort smsVerificationStorePort;
    private final SmsSenderPort smsSenderPort;

    @Override
    public void send(SendSmsCodeCommand command) {
        if (smsVerificationStorePort.hasCooldown(command.phoneNumber())) {
            throw new AccountException(AccountError.SMS_SEND_TOO_SOON);
        }
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        smsVerificationStorePort.save(
                command.phoneNumber(), new SmsVerification(code, 0, Instant.now()));
        try {
            smsSenderPort.send(command.phoneNumber(), command.carrier(), code);
        } catch (RuntimeException exception) {
            smsVerificationStorePort.delete(command.phoneNumber());
            throw exception;
        }
        smsVerificationStorePort.startCooldown(command.phoneNumber());
    }

    @Override
    public void verify(VerifySmsCodeCommand command) {
        SmsVerification verification = smsVerificationStorePort.find(command.phoneNumber())
                .orElseThrow(() -> new AccountException(AccountError.SMS_CODE_NOT_FOUND));
        if (!verification.code().equals(command.code())) {
            SmsVerification updated = verification.incrementAttempts();
            if (updated.attempts() >= MAX_ATTEMPTS) {
                smsVerificationStorePort.delete(command.phoneNumber());
                throw new AccountException(AccountError.SMS_CODE_MAX_ATTEMPTS);
            }
            smsVerificationStorePort.save(command.phoneNumber(), updated);
            throw new AccountException(AccountError.SMS_CODE_INVALID);
        }

        smsVerificationStorePort.delete(command.phoneNumber());
        smsVerificationStorePort.markVerified(command.phoneNumber(), command.purpose());
    }
}
