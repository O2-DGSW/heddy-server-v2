package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.account.model.SmsVerificationPurpose;
import com.heddy.domain.account.model.UserProfile;
import com.heddy.domain.account.port.in.MyProfileResult;
import com.heddy.domain.account.port.in.ProfileUseCase;
import com.heddy.domain.account.port.in.SaveHairProfileCommand;
import com.heddy.domain.account.port.in.UpdateMyProfileCommand;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.HairProfileRepositoryPort;
import com.heddy.domain.account.port.out.SmsVerificationStorePort;
import com.heddy.domain.account.port.out.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService implements ProfileUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final UserProfileRepositoryPort userProfileRepositoryPort;
    private final HairProfileRepositoryPort hairProfileRepositoryPort;
    private final SmsVerificationStorePort smsVerificationStorePort;

    @Override
    public MyProfileResult getProfile(UUID userId) {
        Account account = accountRepositoryPort.findById(userId)
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND));
        UserProfile profile = userProfileRepositoryPort.findByUserId(userId)
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND));
        return result(account, profile);
    }

    @Override
    @Transactional
    public MyProfileResult updateProfile(UpdateMyProfileCommand command) {
        Account account = accountRepositoryPort.findById(command.userId())
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND));
        UserProfile current = userProfileRepositoryPort.findByUserId(command.userId())
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND));
        validateNickname(command);
        validatePhoneChange(command, current);

        UserProfile updated = userProfileRepositoryPort.save(current.update(
                command.nicknamePresent() ? command.nickname() : current.nickname(),
                command.phonePresent() ? command.phone() : current.phone(),
                command.preferredDesignerPresent()
                        ? command.preferredDesigner() : current.preferredDesigner(),
                command.hairCautionsPresent() ? command.hairCautions() : current.hairCautions()));
        consumePhoneVerification(command, current);
        return result(account, updated);
    }

    @Override
    public HairProfile getHairProfile(UUID userId) {
        return hairProfileRepositoryPort.findByUserId(userId)
                .orElseThrow(() -> new AccountException(AccountError.HAIR_PROFILE_NOT_FOUND));
    }

    @Override
    @Transactional
    public HairProfile saveHairProfile(SaveHairProfileCommand command) {
        accountRepositoryPort.findById(command.userId())
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND));
        HairProfile profile = hairProfileRepositoryPort.findByUserId(command.userId())
                .map(current -> current.replace(command.hairType(), command.hairCondition(),
                        command.hairLength(), command.hairThickness(),
                        command.availableCareTimeMinutes()))
                .orElseGet(() -> HairProfile.create(command.userId(), command.hairType(),
                        command.hairCondition(), command.hairLength(), command.hairThickness(),
                        command.availableCareTimeMinutes()));
        return hairProfileRepositoryPort.save(profile);
    }

    private void validateNickname(UpdateMyProfileCommand command) {
        if (command.nicknamePresent()
                && (command.nickname() == null || command.nickname().isBlank())) {
            throw new AccountException(AccountError.PROFILE_INVALID_NICKNAME);
        }
    }

    private void validatePhoneChange(UpdateMyProfileCommand command, UserProfile current) {
        if (!command.phonePresent() || Objects.equals(command.phone(), current.phone())) {
            return;
        }
        if (command.phone() == null) {
            return;
        }
        userProfileRepositoryPort.findUserIdByPhone(command.phone())
                .filter(ownerId -> !ownerId.equals(command.userId()))
                .ifPresent(ownerId -> {
                    throw new AccountException(AccountError.PHONE_ALREADY_EXISTS);
                });
        if (!smsVerificationStorePort.isVerified(
                command.phone(), SmsVerificationPurpose.PHONE_CHANGE)) {
            throw new AccountException(AccountError.PHONE_NOT_VERIFIED);
        }
    }

    private void consumePhoneVerification(UpdateMyProfileCommand command, UserProfile current) {
        if (command.phonePresent() && command.phone() != null
                && !command.phone().equals(current.phone())) {
            smsVerificationStorePort.deleteVerified(
                    command.phone(), SmsVerificationPurpose.PHONE_CHANGE);
        }
    }

    private MyProfileResult result(Account account, UserProfile profile) {
        return new MyProfileResult(account.userId(), account.email(), profile.nickname(),
                profile.phone(), profile.preferredDesigner(), profile.hairCautions(),
                account.status(), account.createdAt(), profile.updatedAt());
    }
}
