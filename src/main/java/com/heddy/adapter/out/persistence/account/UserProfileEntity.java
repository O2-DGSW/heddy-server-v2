package com.heddy.adapter.out.persistence.account;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.account.model.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_profiles")
class UserProfileEntity extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(name = "preferred_designer", length = 30)
    private String preferredDesigner;

    @Column(name = "hair_cautions", columnDefinition = "TEXT")
    private String hairCautions;

    protected UserProfileEntity() {
    }

    UserProfileEntity(UserProfile profile) {
        userId = profile.userId();
        nickname = profile.nickname();
        phone = profile.phone();
        preferredDesigner = profile.preferredDesigner();
        hairCautions = profile.hairCautions();
    }

    UserProfile toDomain() {
        return new UserProfile(userId, nickname, phone, preferredDesigner, hairCautions);
    }
}
