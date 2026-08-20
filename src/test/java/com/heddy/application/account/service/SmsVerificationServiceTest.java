package com.heddy.application.account.service;

import com.heddy.domain.account.model.SmsVerification;
import com.heddy.domain.account.model.SmsVerificationPurpose;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.port.in.SendSmsCodeCommand;
import com.heddy.domain.account.port.in.VerifySmsCodeCommand;
import com.heddy.domain.account.port.out.SmsSenderPort;
import com.heddy.domain.account.port.out.SmsVerificationStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmsVerificationServiceTest {

    private static final String PHONE = "01012345678";

    @Mock SmsVerificationStorePort smsVerificationStorePort;
    @Mock SmsSenderPort smsSenderPort;

    private SmsVerificationService smsVerificationService;

    @BeforeEach
    void setUp() {
        smsVerificationService = new SmsVerificationService(
                smsVerificationStorePort, smsSenderPort);
    }

    @Test
    void sendCreatesSixDigitCodeAndCooldown() {
        smsVerificationService.send(
                new SendSmsCodeCommand(PHONE, "SKT", SmsVerificationPurpose.SIGNUP));

        ArgumentCaptor<SmsVerification> verification = ArgumentCaptor.forClass(SmsVerification.class);
        verify(smsVerificationStorePort).save(org.mockito.ArgumentMatchers.eq(PHONE), verification.capture());
        assertThat(verification.getValue().code()).matches("\\d{6}");
        verify(smsSenderPort).send(PHONE, "SKT", verification.getValue().code());
        verify(smsVerificationStorePort).startCooldown(PHONE);
    }

    @Test
    void cooldownBlocksRepeatedSend() {
        given(smsVerificationStorePort.hasCooldown(PHONE)).willReturn(true);

        assertError(
                () -> smsVerificationService.send(
                        new SendSmsCodeCommand(PHONE, "SKT", SmsVerificationPurpose.SIGNUP)),
                AccountError.SMS_SEND_TOO_SOON);
        verify(smsSenderPort, never()).send(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void senderFailureDeletesUnusableCode() {
        willThrow(new AccountException(AccountError.SMS_SEND_FAILED))
                .given(smsSenderPort).send(
                        org.mockito.ArgumentMatchers.eq(PHONE),
                        org.mockito.ArgumentMatchers.eq("SKT"),
                        org.mockito.ArgumentMatchers.any());

        assertError(
                () -> smsVerificationService.send(
                        new SendSmsCodeCommand(PHONE, "SKT", SmsVerificationPurpose.SIGNUP)),
                AccountError.SMS_SEND_FAILED);
        verify(smsVerificationStorePort).delete(PHONE);
        verify(smsVerificationStorePort, never()).startCooldown(PHONE);
    }

    @Test
    void correctCodeMarksPurposeAsVerified() {
        given(smsVerificationStorePort.find(PHONE))
                .willReturn(Optional.of(new SmsVerification("123456", 0, Instant.now())));

        smsVerificationService.verify(
                new VerifySmsCodeCommand(PHONE, "123456", SmsVerificationPurpose.SIGNUP));

        verify(smsVerificationStorePort).delete(PHONE);
        verify(smsVerificationStorePort).markVerified(PHONE, SmsVerificationPurpose.SIGNUP);
    }

    @Test
    void fifthWrongAttemptDeletesCodeAndLocksVerification() {
        given(smsVerificationStorePort.find(PHONE))
                .willReturn(Optional.of(new SmsVerification("123456", 4, Instant.now())));

        assertError(
                () -> smsVerificationService.verify(
                        new VerifySmsCodeCommand(PHONE, "000000", SmsVerificationPurpose.SIGNUP)),
                AccountError.SMS_CODE_MAX_ATTEMPTS);
        verify(smsVerificationStorePort).delete(PHONE);
    }

    private void assertError(Runnable action, AccountError expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(AccountException.class,
                        exception -> assertThat(exception.error()).isEqualTo(expected));
    }
}
