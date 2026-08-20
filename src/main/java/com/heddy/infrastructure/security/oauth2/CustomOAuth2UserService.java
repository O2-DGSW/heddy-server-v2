package com.heddy.infrastructure.security.oauth2;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.port.out.SocialAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final SocialAccountRepositoryPort socialAccountRepositoryPort;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User delegate = super.loadUser(request);
        OAuthUserInfo userInfo = resolve(
                request.getClientRegistration().getRegistrationId(), delegate);
        if (userInfo.providerId() == null || userInfo.providerId().isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_provider_id"));
        }
        Account account = socialAccountRepositoryPort
                .findByProvider(userInfo.provider(), userInfo.providerId())
                .orElse(null);
        return new OAuth2AccountPrincipal(delegate, userInfo, account);
    }

    private OAuthUserInfo resolve(String registrationId, OAuth2User user) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoUserInfo(user.getAttributes());
            case "naver" -> new NaverUserInfo(user.getAttributes());
            case "google" -> new GoogleUserInfo(user.getAttributes());
            default -> throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"));
        };
    }
}
