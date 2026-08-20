package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SocialAccountJpaRepository extends JpaRepository<SocialAccountEntity, Long> {
    Optional<SocialAccountEntity> findByProviderAndProviderId(SocialProvider provider, String providerId);
}
