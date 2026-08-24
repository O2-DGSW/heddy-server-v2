package com.heddy.adapter.out.persistence.style;

import com.heddy.domain.style.model.StyleTag;
import com.heddy.domain.style.model.StyleTagCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "style_tags")
class StyleTagEntity {

    @Id
    @Column(name = "style_tag_id", nullable = false)
    private UUID styleTagId;

    @Column(name = "tag_name", nullable = false, length = 30)
    private String tagName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StyleTagCategory category;

    protected StyleTagEntity() {
    }

    StyleTag toDomain() {
        return new StyleTag(styleTagId, tagName, category);
    }
}
