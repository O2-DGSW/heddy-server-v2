package com.heddy.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface UsedReauthenticationTokenJpaRepository
        extends JpaRepository<UsedReauthenticationTokenEntity, UUID> {
}
