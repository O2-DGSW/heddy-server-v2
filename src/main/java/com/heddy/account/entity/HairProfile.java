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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "hair_profiles")
public class HairProfile extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

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
    private Short availableCareTimeMinutes;

    protected HairProfile() {
    }

    public HairProfile(User user, HairType hairType, HairCondition hairCondition,
                       HairLength hairLength, HairThickness hairThickness,
                       Short availableCareTimeMinutes) {
        this.id = UuidV7.generate();
        this.user = user;
        this.hairType = hairType;
        this.hairCondition = hairCondition;
        this.hairLength = hairLength;
        this.hairThickness = hairThickness;
        this.availableCareTimeMinutes = availableCareTimeMinutes;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public HairType getHairType() {
        return hairType;
    }

    public HairCondition getHairCondition() {
        return hairCondition;
    }

    public HairLength getHairLength() {
        return hairLength;
    }

    public HairThickness getHairThickness() {
        return hairThickness;
    }

    public Short getAvailableCareTimeMinutes() {
        return availableCareTimeMinutes;
    }
}
