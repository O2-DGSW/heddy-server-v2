package com.heddy.adapter.out.persistence.hairstyle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface HairstyleAssetJpaRepository extends JpaRepository<HairstyleAssetEntity, UUID> {
    @Query(value = """
            SELECT asset.* FROM hairstyle_assets asset
            JOIN hairstyle_recommendation_profiles profile
              ON profile.hairstyle_id = asset.hairstyle_id
            JOIN files thumbnail ON thumbnail.file_id = asset.thumbnail_file_id
            WHERE asset.active = true AND thumbnail.status = 'READY'
            ORDER BY asset.hairstyle_id
            """, nativeQuery = true)
    List<HairstyleAssetEntity> findEligible();
}
