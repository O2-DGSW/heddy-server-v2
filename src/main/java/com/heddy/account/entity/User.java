package com.heddy.account.entity;

import com.heddy.global.entity.BaseEntity;
import com.heddy.global.support.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider;

    @Column(name = "provider_subject", length = 255)
    private String providerSubject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "login_fail_count", nullable = false)
    private short loginFailCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected User() {
    }

    private User(String email, String passwordHash, String nickname,
                 AuthProvider authProvider, String providerSubject) {
        this.id = UuidV7.generate();
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.authProvider = authProvider;
        this.providerSubject = providerSubject;
        this.status = UserStatus.ACTIVE;
        this.loginFailCount = 0;
    }

    /** 이메일 가입. ck_users_credential 이 password_hash 를 요구한다. */
    public static User ofEmail(String email, String passwordHash, String nickname) {
        return new User(email, Objects.requireNonNull(passwordHash, "passwordHash"),
                nickname, AuthProvider.EMAIL, null);
    }

    /** 소셜 가입. ck_users_credential 이 provider_subject 를 요구한다. */
    public static User ofSocial(String email, String nickname, AuthProvider authProvider, String providerSubject) {
        if (authProvider == AuthProvider.EMAIL) {
            throw new IllegalArgumentException("소셜 가입에는 EMAIL 을 쓸 수 없다");
        }
        return new User(email, null, nickname, authProvider,
                Objects.requireNonNull(providerSubject, "providerSubject"));
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public UserStatus getStatus() {
        return status;
    }

    public short getLoginFailCount() {
        return loginFailCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
