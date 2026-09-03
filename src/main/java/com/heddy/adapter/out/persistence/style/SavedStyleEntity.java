package com.heddy.adapter.out.persistence.style;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.style.model.SavedStyle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "saved_styles")
class SavedStyleEntity extends BaseEntity {

    @Id
    @Column(name = "saved_style_id", nullable = false, updatable = false)
    private UUID savedStyleId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "style_name", nullable = false, length = 100, updatable = false)
    private String styleName;

    @Column(name = "image_url", length = 2048, updatable = false)
    private String imageUrl;

    @Column(length = 500, updatable = false)
    private String reason;

    @Column(name = "hairstyle_id", updatable = false)
    private UUID hairstyleId;

    @Column(name = "color_id", updatable = false)
    private UUID colorId;

    @Column(name = "capture_id", updatable = false)
    private UUID captureId;

    @Column(length = 500)
    private String memo;

    protected SavedStyleEntity() {
    }

    SavedStyleEntity(SavedStyle savedStyle) {
        savedStyleId = savedStyle.savedStyleId();
        userId = savedStyle.userId();
        styleName = savedStyle.styleName();
        imageUrl = savedStyle.imageUrl();
        reason = savedStyle.reason();
        hairstyleId = savedStyle.hairstyleId();
        colorId = savedStyle.colorId();
        captureId = savedStyle.captureId();
        memo = savedStyle.memo();
    }

    void updateMemo(String updatedMemo) {
        memo = updatedMemo;
    }

    SavedStyle toDomain() {
        return new SavedStyle(
                savedStyleId, userId, styleName, imageUrl, reason,
                hairstyleId, colorId, captureId, memo, getCreatedAt());
    }
}
