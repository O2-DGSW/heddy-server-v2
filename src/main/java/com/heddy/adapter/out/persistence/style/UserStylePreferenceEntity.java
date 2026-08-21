package com.heddy.adapter.out.persistence.style;

import com.heddy.domain.style.model.UserStylePreference;
import com.heddy.domain.style.model.UserStylePreference.PreferenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_style_preferences")
@EntityListeners(AuditingEntityListener.class)
class UserStylePreferenceEntity {

    @Id
    @Column(name = "preference_id", nullable = false)
    private UUID preferenceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "style_tag_id", nullable = false)
    private UUID styleTagId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_type", nullable = false, length = 10)
    private PreferenceType preferenceType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserStylePreferenceEntity() {
    }

    UserStylePreferenceEntity(UserStylePreference preference) {
        preferenceId = preference.preferenceId();
        userId = preference.userId();
        styleTagId = preference.styleTagId();
        preferenceType = preference.preferenceType();
    }

    UserStylePreference toDomain() {
        return new UserStylePreference(
                preferenceId, userId, styleTagId, preferenceType, createdAt);
    }
}
