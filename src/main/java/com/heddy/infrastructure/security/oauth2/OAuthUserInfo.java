package com.heddy.infrastructure.security.oauth2;

import com.heddy.domain.account.model.SocialProvider;

interface OAuthUserInfo {
    SocialProvider provider();
    String providerId();
}
