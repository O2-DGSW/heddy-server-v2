package com.heddy.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ConsentHistoryJpaRepository extends JpaRepository<ConsentHistoryEntity, UUID> {
}
