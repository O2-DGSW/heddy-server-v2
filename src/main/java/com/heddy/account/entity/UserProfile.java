package com.heddy.account.entity;

import com.heddy.global.entity.BaseEntity;
import com.heddy.global.support.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "preferred_designer", length = 50)
    private String preferredDesigner;

    @Column(name = "hair_cautions", length = 500)
    private String hairCautions;

    protected UserProfile() {
    }

    public UserProfile(User user, String phone, String preferredDesigner, String hairCautions) {
        this.id = UuidV7.generate();
        this.user = user;
        this.phone = phone;
        this.preferredDesigner = preferredDesigner;
        this.hairCautions = hairCautions;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getPhone() {
        return phone;
    }

    public String getPreferredDesigner() {
        return preferredDesigner;
    }

    public String getHairCautions() {
        return hairCautions;
    }
}
