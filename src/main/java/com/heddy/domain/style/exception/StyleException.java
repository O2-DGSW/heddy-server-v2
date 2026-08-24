package com.heddy.domain.style.exception;

import java.util.List;
import java.util.UUID;

public class StyleException extends RuntimeException {

    private final StyleError error;
    private final List<UUID> invalidPreferredTagIds;
    private final List<UUID> invalidExcludedTagIds;

    public StyleException(StyleError error) {
        this(error, List.of(), List.of());
    }

    private StyleException(
            StyleError error,
            List<UUID> invalidPreferredTagIds,
            List<UUID> invalidExcludedTagIds
    ) {
        super(error.message());
        this.error = error;
        this.invalidPreferredTagIds = List.copyOf(invalidPreferredTagIds);
        this.invalidExcludedTagIds = List.copyOf(invalidExcludedTagIds);
    }

    public static StyleException invalidTagIds(
            List<UUID> invalidPreferredTagIds,
            List<UUID> invalidExcludedTagIds
    ) {
        return new StyleException(
                StyleError.INVALID_TAG_IDS, invalidPreferredTagIds, invalidExcludedTagIds);
    }

    public StyleError error() {
        return error;
    }

    public List<UUID> invalidPreferredTagIds() {
        return invalidPreferredTagIds;
    }

    public List<UUID> invalidExcludedTagIds() {
        return invalidExcludedTagIds;
    }
}
