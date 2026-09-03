package com.heddy.adapter.out.persistence.style;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
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

    Optional<SavedStyleEntity> findBySavedStyleIdAndUserId(UUID savedStyleId, UUID userId);

    Page<SavedStyleEntity> findAllByUserId(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    boolean existsByUserIdAndStyleNameAndImageUrl(UUID userId, String styleName, String imageUrl);

    long deleteBySavedStyleIdAndUserId(UUID savedStyleId, UUID userId);

    @Modifying
    @Query(value = "DELETE FROM share_saved_styles WHERE saved_style_id = :savedStyleId",
            nativeQuery = true)
    void deleteShareLinks(@Param("savedStyleId") UUID savedStyleId);

    void deleteAllByUserId(UUID userId);
}
