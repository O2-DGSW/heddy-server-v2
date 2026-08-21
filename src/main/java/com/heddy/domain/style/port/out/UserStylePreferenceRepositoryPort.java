package com.heddy.domain.style.port.out;

import com.heddy.domain.style.model.UserStylePreference;

import java.util.List;
import java.util.UUID;

public interface UserStylePreferenceRepositoryPort {
    List<UserStylePreference> findAllByUserId(UUID userId);

    List<UserStylePreference> replace(
            UUID userId,
            List<UserStylePreference> preferences
    );
}
