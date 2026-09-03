package com.heddy.adapter.out.persistence.style;

import com.heddy.domain.style.model.HairColor;
import com.heddy.domain.style.port.out.HairColorRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HairColorPersistenceAdapter implements HairColorRepositoryPort {

    private final HairColorJpaRepository repository;

    @Override
    public List<HairColor> findAllActive() {
        return repository.findAllByActiveTrueOrderBySortOrderAscColorIdAsc().stream()
                .map(HairColorEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<HairColor> findById(UUID colorId) {
        return repository.findById(colorId).map(HairColorEntity::toDomain);
    }
}
