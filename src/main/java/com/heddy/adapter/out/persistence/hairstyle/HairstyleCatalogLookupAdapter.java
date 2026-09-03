package com.heddy.adapter.out.persistence.hairstyle;

import com.heddy.domain.style.model.CatalogHairstyle;
import com.heddy.domain.style.port.out.HairstyleCatalogLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HairstyleCatalogLookupAdapter implements HairstyleCatalogLookupPort {

    private final HairstyleAssetJpaRepository assetRepository;

    @Override
    public Optional<CatalogHairstyle> findActiveById(UUID hairstyleId) {
        return assetRepository.findById(hairstyleId)
                .filter(HairstyleAssetEntity::active)
                .map(asset -> new CatalogHairstyle(
                        asset.hairstyleId(), asset.styleName(), asset.thumbnailFileId()));
    }

    @Override
    public Map<UUID, UUID> findThumbnailFileIds(Collection<UUID> hairstyleIds) {
        if (hairstyleIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> thumbnails = new HashMap<>();
        for (HairstyleAssetEntity asset : assetRepository.findAllById(hairstyleIds)) {
            if (asset.thumbnailFileId() != null) {
                thumbnails.put(asset.hairstyleId(), asset.thumbnailFileId());
            }
        }
        return thumbnails;
    }
}
