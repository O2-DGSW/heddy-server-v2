package com.heddy.adapter.out.persistence.style;

import com.heddy.domain.style.model.UserStylePreference;
import com.heddy.domain.style.port.out.UserStylePreferenceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserStylePreferencePersistenceAdapter
        implements UserStylePreferenceRepositoryPort {

    private final UserStylePreferenceJpaRepository repository;

    @Override
    public List<UserStylePreference> findAllByUserId(UUID userId) {
        return repository.findAllByUserIdOrderByPreferenceTypeAscStyleTagIdAsc(userId).stream()
                .map(UserStylePreferenceEntity::toDomain)
                .toList();
    }

    @Override
    public List<UserStylePreference> replace(
            UUID userId,
            List<UserStylePreference> preferences
    ) {
        repository.deleteAllByUserId(userId);
        repository.flush();
        if (preferences.isEmpty()) {
            return List.of();
        }
        return repository.saveAllAndFlush(preferences.stream()
                        .map(UserStylePreferenceEntity::new)
                        .toList()).stream()
                .map(UserStylePreferenceEntity::toDomain)
                .toList();
    }
}
