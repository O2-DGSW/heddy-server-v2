package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.HairProfile;

import java.util.Optional;
import java.util.UUID;

public interface HairProfileRepositoryPort {
    Optional<HairProfile> findByUserId(UUID userId);
    HairProfile save(HairProfile hairProfile);
    void deleteByUserId(UUID userId);
}
