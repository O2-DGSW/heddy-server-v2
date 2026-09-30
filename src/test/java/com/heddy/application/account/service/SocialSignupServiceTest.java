package com.heddy.application.account.service;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.model.ConsentDecision;
import com.heddy.domain.account.model.ConsentType;
import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.account.port.in.SignupHairProfileCommand;
import com.heddy.domain.account.port.in.SocialSignupCommand;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.ConsentHistoryRepositoryPort;
import com.heddy.domain.account.port.out.HairProfileRepositoryPort;
import com.heddy.domain.account.port.out.SocialTokenVerifierPort;
import com.heddy.domain.account.port.out.UserProfileRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocialSignupServiceTest {

    @Mock SocialTokenVerifierPort socialTokenVerifierPort;
    @Mock AccountRepositoryPort accountRepositoryPort;
    @Mock UserProfileRepositoryPort userProfileRepositoryPort;
    @Mock HairProfileRepositoryPort hairProfileRepositoryPort;
    @Mock ConsentHistoryRepositoryPort consentHistoryRepositoryPort;
    @Mock SessionTokenService sessionTokenService;
    @Mock SignupPhoneVerificationService signupPhoneVerificationService;

    private SocialSignupService service;

    @BeforeEach
    void setUp() {
        service = new SocialSignupService(socialTokenVerifierPort, accountRepositoryPort,
                userProfileRepositoryPort, hairProfileRepositoryPort,
                consentHistoryRepositoryPort, sessionTokenService,
                signupPhoneVerificationService);
    }

    @Test
    void savesHairProfileWhenProvided() {
        SignupHairProfileCommand hairProfile = new SignupHairProfileCommand(
                HairProfile.HairType.WAVY, HairProfile.HairCondition.NORMAL,
                HairProfile.HairLength.BELOW_SHOULDER,
                HairProfile.HairThickness.NORMAL, 20);
        SocialSignupCommand command = new SocialSignupCommand(
                AuthProvider.GOOGLE, "provider-token", "헤디", null,
                requiredConsents(), hairProfile);
        given(socialTokenVerifierPort.verify(AuthProvider.GOOGLE, "provider-token"))
                .willReturn(Optional.of(() -> "provider-subject"));
        given(accountRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(userProfileRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(hairProfileRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.signup(command);

        ArgumentCaptor<Account> account = ArgumentCaptor.forClass(Account.class);
        verify(accountRepositoryPort).save(account.capture());
        ArgumentCaptor<HairProfile> saved = ArgumentCaptor.forClass(HairProfile.class);
        verify(hairProfileRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().userId()).isEqualTo(account.getValue().userId());
        assertThat(saved.getValue().hairType()).isEqualTo(HairProfile.HairType.WAVY);
        assertThat(saved.getValue().availableCareTimeMinutes()).isEqualTo(20);
    }

    @Test
    void skipsHairProfileWhenNotProvided() {
        SocialSignupCommand command = new SocialSignupCommand(
                AuthProvider.GOOGLE, "provider-token", "헤디", null,
                requiredConsents(), null);
        given(socialTokenVerifierPort.verify(AuthProvider.GOOGLE, "provider-token"))
                .willReturn(Optional.of(() -> "provider-subject"));
        given(accountRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(userProfileRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.signup(command);

        verify(hairProfileRepositoryPort, never()).save(any());
    }

    private List<ConsentDecision> requiredConsents() {
        return List.of(
                new ConsentDecision(ConsentType.TERMS_OF_SERVICE, true, "2026-08-01"),
                new ConsentDecision(ConsentType.PRIVACY_POLICY, true, "2026-08-01"));
    }
}
