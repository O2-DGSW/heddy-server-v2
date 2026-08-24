package com.heddy.adapter.out.persistence.treatment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TreatmentPhotoJpaRepository extends JpaRepository<TreatmentPhotoEntity, UUID> {

    List<TreatmentPhotoEntity> findByRecordIdOrderByCreatedAtAsc(UUID recordId);
}
