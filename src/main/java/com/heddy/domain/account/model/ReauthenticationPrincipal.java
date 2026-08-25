package com.heddy.domain.account.model;

import java.util.UUID;

public record ReauthenticationPrincipal(UUID userId, UUID tokenId) {
}
