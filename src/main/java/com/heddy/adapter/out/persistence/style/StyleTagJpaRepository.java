package com.heddy.adapter.out.persistence.style;

import com.heddy.domain.style.model.StyleTagCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface StyleTagJpaRepository extends JpaRepository<StyleTagEntity, UUID> {
    List<StyleTagEntity> findAllByOrderByCategoryAscTagNameAsc();

    List<StyleTagEntity> findAllByCategoryOrderByTagNameAsc(StyleTagCategory category);
}
