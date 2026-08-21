package com.heddy.adapter.out.persistence.style;

import com.heddy.domain.style.model.StyleTag;
import com.heddy.domain.style.model.StyleTagCategory;
import com.heddy.domain.style.port.out.StyleTagRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StyleTagPersistenceAdapter implements StyleTagRepositoryPort {

    private final StyleTagJpaRepository repository;

    @Override
    public List<StyleTag> findAll(StyleTagCategory category) {
        List<StyleTagEntity> entities = category == null
                ? repository.findAllByOrderByCategoryAscTagNameAsc()
                : repository.findAllByCategoryOrderByTagNameAsc(category);
        return entities.stream().map(StyleTagEntity::toDomain).toList();
    }

    @Override
    public List<StyleTag> findAllByIds(Collection<UUID> styleTagIds) {
        return repository.findAllById(styleTagIds).stream()
                .map(StyleTagEntity::toDomain)
                .toList();
    }
}
