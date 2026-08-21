package com.heddy.domain.style.model;

import java.util.UUID;

public record StyleTag(
        UUID styleTagId,
        String tagName,
        StyleTagCategory category
) {
}
