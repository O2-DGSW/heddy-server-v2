package com.heddy.adapter.out.persistence.style;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface SavedStyleJpaRepository extends JpaRepository<SavedStyleEntity, UUID> {

    List<SavedStyleEntity> findAllByUserIdAndSavedStyleIdInOrderByCreatedAtDescSavedStyleIdDesc(
            UUID userId,
            Collection<UUID> savedStyleIds
    );
}
