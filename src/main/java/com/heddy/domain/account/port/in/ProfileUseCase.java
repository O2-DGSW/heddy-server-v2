package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.HairProfile;

import java.util.UUID;

public interface ProfileUseCase {
    MyProfileResult getProfile(UUID userId);
    MyProfileResult updateProfile(UpdateMyProfileCommand command);
    HairProfile getHairProfile(UUID userId);
    HairProfile saveHairProfile(SaveHairProfileCommand command);
}
