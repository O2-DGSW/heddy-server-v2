package com.heddy.application.account.service;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.SmsVerificationPurpose;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.port.in.AuthTokens;
import com.heddy.domain.account.port.in.SocialSignupCommand;
import com.heddy.domain.account.port.in.SocialSignupUseCase;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.AuthTokenPort;
import com.heddy.domain.account.port.out.PendingSocialLoginStorePort;
import com.heddy.domain.account.port.out.RefreshTokenStorePort;
import com.heddy.domain.account.port.out.SmsVerificationStorePort;
import com.heddy.domain.account.port.out.SocialAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialSignupService implements SocialSignupUseCase {

    private final PendingSocialLoginStorePort pendingSocialLoginStorePort;
    private final SmsVerificationStorePort smsVerificationStorePort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final SocialAccountRepositoryPort socialAccountRepositoryPort;
    private final AuthTokenPort authTokenPort;
    private final RefreshTokenStorePort refreshTokenStorePort;

    @Override
    @Transactional
    public AuthTokens signup(SocialSignupCommand command) {
        PendingSocialLoginStorePort.PendingSocialLogin pending = pendingSocialLoginStorePort
                .find(command.pendingToken())
                .orElseThrow(() -> new AccountException(AccountError.SOCIAL_PENDING_EXPIRED));
        if (socialAccountRepositoryPort.findByProvider(pending.provider(), pending.providerId()).isPresent()) {
            throw new AccountException(AccountError.SOCIAL_ALREADY_LINKED);
        }
        if (accountRepositoryPort.existsByPhoneNumber(command.phoneNumber())) {
            throw new AccountException(AccountError.PHONE_DUPLICATED);
        }
        if (!smsVerificationStorePort.isVerified(command.phoneNumber(), SmsVerificationPurpose.SIGNUP)) {
            throw new AccountException(AccountError.PHONE_NOT_VERIFIED);
        }

        Account account = accountRepositoryPort.save(Account.social(command.name(), command.phoneNumber()));
        socialAccountRepositoryPort.link(account.id(), pending.provider(), pending.providerId());
        pendingSocialLoginStorePort.delete(command.pendingToken());
        smsVerificationStorePort.deleteVerified(command.phoneNumber(), SmsVerificationPurpose.SIGNUP);

        String accessToken = authTokenPort.createAccessToken(account.id(), account.role());
        String refreshToken = authTokenPort.createRefreshToken(account.id(), account.role());
        refreshTokenStorePort.save(account.id(), refreshToken);
        return new AuthTokens(accessToken, refreshToken);
    }
}
