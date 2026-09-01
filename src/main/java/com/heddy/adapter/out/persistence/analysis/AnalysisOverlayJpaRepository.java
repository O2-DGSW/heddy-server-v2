package com.heddy.adapter.out.persistence.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AnalysisOverlayJpaRepository extends JpaRepository<AnalysisOverlayEntity, UUID> {

    List<AnalysisOverlayEntity> findByAnalysisIdOrderByOverlayType(UUID analysisId);
}
