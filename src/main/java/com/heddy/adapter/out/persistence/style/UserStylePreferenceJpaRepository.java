package com.heddy.adapter.out.persistence.style;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface UserStylePreferenceJpaRepository
        extends JpaRepository<UserStylePreferenceEntity, UUID> {
    List<UserStylePreferenceEntity> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    void deleteAllByUserId(UUID userId);
}
