package com.heddy.adapter.out.persistence.style;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SavedStyleJpaRepository extends JpaRepository<SavedStyleEntity, UUID> {

    List<SavedStyleEntity> findAllByUserIdAndSavedStyleIdInOrderByCreatedAtDescSavedStyleIdDesc(
            UUID userId,
            Collection<UUID> savedStyleIds
    );

    List<SavedStyleEntity> findAllByUserIdOrderByCreatedAtDescSavedStyleIdDesc(UUID userId);

    Optional<SavedStyleEntity> findBySavedStyleIdAndUserId(UUID savedStyleId, UUID userId);

    int deleteBySavedStyleId(UUID savedStyleId);

    void deleteAllByUserId(UUID userId);

    @Query(value = """
            SELECT DISTINCT hairstyle_id FROM saved_styles
            WHERE user_id = :userId AND hairstyle_id IS NOT NULL
            ORDER BY hairstyle_id
            """, nativeQuery = true)
    List<UUID> findHairstyleIdsByUserId(@Param("userId") UUID userId);
}
