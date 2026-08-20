package com.heddy.adapter.out.persistence.account;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.account.model.AccountRole;
import com.heddy.domain.account.model.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class AccountEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", unique = true, length = 20)
    private String loginId;

    @Column(name = "password", length = 100)
    private String encodedPassword;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", nullable = false, unique = true, length = 13)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    protected AccountEntity() {
    }

    AccountEntity(
            String loginId,
            String encodedPassword,
            String name,
            String phoneNumber,
            AccountRole role,
            AccountStatus status,
            boolean phoneVerified
    ) {
        this.loginId = loginId;
        this.encodedPassword = encodedPassword;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.status = status;
        this.phoneVerified = phoneVerified;
    }

    void updatePassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    Long id() {
        return id;
    }

    String loginId() {
        return loginId;
    }

    String encodedPassword() {
        return encodedPassword;
    }

    String name() {
        return name;
    }

    String phoneNumber() {
        return phoneNumber;
    }

    AccountRole role() {
        return role;
    }

    AccountStatus status() {
        return status;
    }

    boolean phoneVerified() {
        return phoneVerified;
    }
}
