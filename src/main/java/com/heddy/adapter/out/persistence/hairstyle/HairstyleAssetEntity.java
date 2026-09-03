package com.heddy.adapter.out.persistence.hairstyle;

import com.heddy.adapter.out.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "hairstyle_assets")
class HairstyleAssetEntity extends BaseEntity {
    @Id
    @Column(name = "hairstyle_id", nullable = false, updatable = false)
    private UUID hairstyleId;

    @Column(name = "style_name", nullable = false, length = 100)
    private String styleName;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "thumbnail_file_id")
    private UUID thumbnailFileId;

    @Column(name = "ar_mode", nullable = false, length = 30)
    private String arMode;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "asset_version", nullable = false, length = 30)
    private String assetVersion;

    protected HairstyleAssetEntity() { }

    UUID hairstyleId() { return hairstyleId; }
    String styleName() { return styleName; }
    String category() { return category; }
    UUID thumbnailFileId() { return thumbnailFileId; }
    String arMode() { return arMode; }
    boolean active() { return active; }
    String assetVersion() { return assetVersion; }
}
