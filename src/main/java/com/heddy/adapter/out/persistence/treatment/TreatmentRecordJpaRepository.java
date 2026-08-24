package com.heddy.adapter.out.persistence.treatment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TreatmentRecordJpaRepository extends JpaRepository<TreatmentRecordEntity, UUID> {
}
