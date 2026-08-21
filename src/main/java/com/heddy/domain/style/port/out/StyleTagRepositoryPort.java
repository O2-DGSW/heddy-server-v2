package com.heddy.domain.style.port.out;

import com.heddy.domain.style.model.StyleTag;
import com.heddy.domain.style.model.StyleTagCategory;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StyleTagRepositoryPort {
    List<StyleTag> findAll(StyleTagCategory category);

    List<StyleTag> findAllByIds(Collection<UUID> styleTagIds);
}
