package com.heddy.account.entity;

import com.heddy.global.entity.BaseEntity;
import com.heddy.global.support.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_style_preferences")
public class UserStylePreference extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_tag_id", nullable = false, updatable = false)
    private StyleTag styleTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_type", nullable = false, length = 10)
    private PreferenceType preferenceType;

    protected UserStylePreference() {
    }

    public UserStylePreference(User user, StyleTag styleTag, PreferenceType preferenceType) {
        this.id = UuidV7.generate();
        this.user = user;
        this.styleTag = styleTag;
        this.preferenceType = preferenceType;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public StyleTag getStyleTag() {
        return styleTag;
    }

    public PreferenceType getPreferenceType() {
        return preferenceType;
    }
}
