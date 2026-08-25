package com.heddy.adapter.out.persistence.treatment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface TreatmentRecordJpaRepository extends JpaRepository<TreatmentRecordEntity, UUID> {

    Optional<TreatmentRecordEntity> findByRecordIdAndUserId(UUID recordId, UUID userId);
}
