package com.heddy.adapter.out.persistence.account;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.account.model.HairProfile.HairCondition;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.account.model.HairProfile.HairThickness;
import com.heddy.domain.account.model.HairProfile.HairType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "hair_profiles")
class HairProfileEntity extends BaseEntity {

    @Id
    @Column(name = "hair_profile_id", nullable = false)
    private UUID hairProfileId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "hair_type", nullable = false, length = 20)
    private HairType hairType;

    @Enumerated(EnumType.STRING)
    @Column(name = "hair_condition", nullable = false, length = 20)
    private HairCondition hairCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "hair_length", nullable = false, length = 20)
    private HairLength hairLength;

    @Enumerated(EnumType.STRING)
    @Column(name = "hair_thickness", nullable = false, length = 20)
    private HairThickness hairThickness;

    @Column(name = "available_care_time_minutes")
    private Integer availableCareTimeMinutes;

    protected HairProfileEntity() {
    }

    HairProfileEntity(HairProfile profile) {
        hairProfileId = profile.hairProfileId();
        userId = profile.userId();
        update(profile);
    }

    void update(HairProfile profile) {
        hairType = profile.hairType();
        hairCondition = profile.hairCondition();
        hairLength = profile.hairLength();
        hairThickness = profile.hairThickness();
        availableCareTimeMinutes = profile.availableCareTimeMinutes();
    }

    HairProfile toDomain() {
        return new HairProfile(hairProfileId, userId, hairType, hairCondition, hairLength,
                hairThickness, availableCareTimeMinutes, getCreatedAt(), getUpdatedAt());
    }
}
