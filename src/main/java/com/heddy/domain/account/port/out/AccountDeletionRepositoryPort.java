package com.heddy.domain.account.port.out;

import com.heddy.domain.account.model.AccountDeletionRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountDeletionRepositoryPort {

    AccountDeletionRequest save(AccountDeletionRequest request);

    Optional<AccountDeletionRequest> findProcessingByUserId(UUID userId);

    List<AccountDeletionRequest> findProcessingBatch(int limit);

    boolean consumeReauthenticationToken(UUID tokenId, UUID userId, Instant usedAt);
}
