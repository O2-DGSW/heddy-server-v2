package com.heddy.adapter.out.persistence.analysis;

import com.heddy.domain.analysis.model.AnalysisOverlay;
import com.heddy.domain.analysis.port.out.AnalysisOverlayRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AnalysisOverlayPersistenceAdapter implements AnalysisOverlayRepositoryPort {

    private final AnalysisOverlayJpaRepository overlayRepository;

    @Override
    public AnalysisOverlay insert(AnalysisOverlay overlay) {
        return overlayRepository.saveAndFlush(new AnalysisOverlayEntity(overlay)).toDomain();
    }

    @Override
    public List<AnalysisOverlay> findByAnalysisId(UUID analysisId) {
        return overlayRepository.findByAnalysisIdOrderByOverlayType(analysisId).stream()
                .map(AnalysisOverlayEntity::toDomain)
                .toList();
    }
}
