package com.heddy.adapter.out.persistence.account;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.model.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_provider_subject",
                columnNames = {"auth_provider", "provider_subject"})
)
public class AccountEntity extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider;

    @Column(name = "provider_subject", length = 255)
    private String providerSubject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "login_fail_count", nullable = false)
    private short loginFailCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected AccountEntity() {
    }

    AccountEntity(Account account) {
        update(account);
    }

    void update(Account account) {
        userId = account.userId();
        email = account.email();
        passwordHash = account.passwordHash();
        authProvider = account.authProvider();
        providerSubject = account.providerSubject();
        status = account.status();
        loginFailCount = (short) account.loginFailCount();
        lockedUntil = account.lockedUntil();
    }

    void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    Account toDomain() {
        return new Account(userId, email, passwordHash, authProvider, providerSubject,
                status, loginFailCount, lockedUntil);
    }

    UUID userId() {
        return userId;
    }
}
