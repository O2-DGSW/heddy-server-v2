package com.heddy.account.entity;

import com.heddy.global.entity.BaseEntity;
import com.heddy.global.support.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "style_tags")
public class StyleTag extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tag_name", nullable = false, length = 30)
    private String tagName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type", nullable = false, length = 20)
    private StyleTagType tagType;

    protected StyleTag() {
    }

    public StyleTag(String tagName, StyleTagType tagType) {
        this.id = UuidV7.generate();
        this.tagName = tagName;
        this.tagType = tagType;
    }

    public UUID getId() {
        return id;
    }

    public String getTagName() {
        return tagName;
    }

    public StyleTagType getTagType() {
        return tagType;
    }
}
