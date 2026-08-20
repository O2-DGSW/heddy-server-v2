package com.heddy.application.account.service;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.SmsVerificationPurpose;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.port.in.SignupAccountCommand;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.PasswordEncoderPort;
import com.heddy.domain.account.port.out.SmsVerificationStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SignupAccountServiceTest {

    @Mock AccountRepositoryPort accountRepositoryPort;
    @Mock SmsVerificationStorePort smsVerificationStorePort;
    @Mock PasswordEncoderPort passwordEncoderPort;

    private SignupAccountService signupAccountService;

    @BeforeEach
    void setUp() {
        signupAccountService = new SignupAccountService(
                accountRepositoryPort, smsVerificationStorePort, passwordEncoderPort);
    }

    @Test
    void verifiedPhoneCreatesAccountWithEncodedPassword() {
        SignupAccountCommand command = command();
        given(smsVerificationStorePort.isVerified(
                command.phoneNumber(), SmsVerificationPurpose.SIGNUP)).willReturn(true);
        given(passwordEncoderPort.encode(command.password())).willReturn("encoded");

        signupAccountService.signup(command);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().encodedPassword()).isEqualTo("encoded");
        assertThat(captor.getValue().phoneVerified()).isTrue();
        verify(smsVerificationStorePort).deleteVerified(
                command.phoneNumber(), SmsVerificationPurpose.SIGNUP);
    }

    @Test
    void duplicateLoginIdIsRejected() {
        given(accountRepositoryPort.existsByLoginId("mola")).willReturn(true);

        assertThatThrownBy(() -> signupAccountService.signup(command()))
                .isInstanceOfSatisfying(AccountException.class,
                        exception -> assertThat(exception.error())
                                .isEqualTo(AccountError.LOGIN_ID_DUPLICATED));
    }

    @Test
    void unverifiedPhoneIsRejected() {
        SignupAccountCommand command = command();
        given(smsVerificationStorePort.isVerified(
                command.phoneNumber(), SmsVerificationPurpose.SIGNUP)).willReturn(false);

        assertThatThrownBy(() -> signupAccountService.signup(command))
                .isInstanceOfSatisfying(AccountException.class,
                        exception -> assertThat(exception.error())
                                .isEqualTo(AccountError.PHONE_NOT_VERIFIED));
    }

    private SignupAccountCommand command() {
        return new SignupAccountCommand("mola", "password!1", "몰라", "01012345678");
    }
}
