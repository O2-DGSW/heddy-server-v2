package com.heddy.infrastructure.security.oauth2;

import com.heddy.domain.account.model.SocialProvider;

import java.util.Map;

final class KakaoUserInfo implements OAuthUserInfo {

    private final Map<String, Object> attributes;

    KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public String providerId() {
        Object id = attributes.get("id");
        return id == null ? null : id.toString();
    }
}
