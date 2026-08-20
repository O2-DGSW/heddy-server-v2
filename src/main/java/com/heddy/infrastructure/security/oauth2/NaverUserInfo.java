package com.heddy.infrastructure.security.oauth2;

import com.heddy.domain.account.model.SocialProvider;

import java.util.Map;

final class NaverUserInfo implements OAuthUserInfo {

    private final Map<String, Object> attributes;

    NaverUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.NAVER;
    }

    @Override
    public String providerId() {
        Object response = attributes.get("response");
        if (!(response instanceof Map<?, ?> responseAttributes)) {
            return null;
        }
        Object id = responseAttributes.get("id");
        return id == null ? null : id.toString();
    }
}
