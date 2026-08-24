package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepositoryPort {
    UserProfile save(UserProfile profile);
    Optional<UserProfile> findByUserId(UUID userId);
    Optional<UUID> findUserIdByPhone(String phone);
    boolean existsByPhone(String phone);
}
