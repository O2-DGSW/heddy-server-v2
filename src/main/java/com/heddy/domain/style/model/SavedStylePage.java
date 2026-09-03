package com.heddy.domain.style.model;

import java.util.List;

public record SavedStylePage(
        List<SavedStyle> items,
        long totalElements
) {
    public SavedStylePage {
        items = List.copyOf(items);
    }
}
