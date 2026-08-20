package com.heddy.infrastructure.security.oauth2;

import com.heddy.domain.account.model.Account;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

final class OAuth2AccountPrincipal implements OAuth2User {

    private final OAuth2User delegate;
    private final OAuthUserInfo userInfo;
    private final Account account;

    OAuth2AccountPrincipal(OAuth2User delegate, OAuthUserInfo userInfo, Account account) {
        this.delegate = delegate;
        this.userInfo = userInfo;
        this.account = account;
    }

    OAuthUserInfo userInfo() {
        return userInfo;
    }

    Account account() {
        return account;
    }

    boolean isNewAccount() {
        return account == null;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (account == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name()));
    }

    @Override
    public String getName() {
        return userInfo.providerId();
    }
}
