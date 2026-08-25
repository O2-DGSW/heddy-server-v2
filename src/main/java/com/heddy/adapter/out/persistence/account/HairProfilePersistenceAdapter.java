package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.account.port.out.HairProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HairProfilePersistenceAdapter implements HairProfileRepositoryPort {

    private final HairProfileJpaRepository repository;

    @Override
    public Optional<HairProfile> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(HairProfileEntity::toDomain);
    }

    @Override
    public HairProfile save(HairProfile hairProfile) {
        HairProfileEntity entity = repository.findByUserId(hairProfile.userId())
                .orElseGet(() -> new HairProfileEntity(hairProfile));
        entity.update(hairProfile);
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    public void deleteByUserId(UUID userId) {
        repository.deleteByUserId(userId);
        repository.flush();
    }
}
