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

    @Column(name = "image_url", nullable = false, length = 2048, updatable = false)
    private String imageUrl;

    @Column(nullable = false, length = 500, updatable = false)
    private String reason;

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
        memo = savedStyle.memo();
    }

    void updateMemo(String memo) {
        this.memo = memo;
    }

    SavedStyle toDomain() {
        return new SavedStyle(
                savedStyleId, userId, styleName, imageUrl, reason, memo,
                getCreatedAt(), getUpdatedAt());
    }
}
