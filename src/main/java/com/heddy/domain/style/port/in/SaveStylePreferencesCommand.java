package com.heddy.domain.style.port.in;

import java.util.List;
import java.util.UUID;

public record SaveStylePreferencesCommand(
        UUID userId,
        List<UUID> preferredTagIds,
        List<UUID> excludedTagIds
) {
    public SaveStylePreferencesCommand {
        preferredTagIds = List.copyOf(preferredTagIds);
        excludedTagIds = List.copyOf(excludedTagIds);
    }
}
