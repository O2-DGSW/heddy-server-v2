package com.heddy.infrastructure.security.oauth2;

import com.heddy.domain.account.model.SocialProvider;

import java.util.Map;

final class GoogleUserInfo implements OAuthUserInfo {

    private final Map<String, Object> attributes;

    GoogleUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public String providerId() {
        return stringValue(attributes.get("sub"));
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
