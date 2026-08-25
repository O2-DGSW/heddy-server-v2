package com.heddy.adapter.out.persistence.account;

import com.heddy.domain.account.model.AccountDeletionRequest;
import com.heddy.domain.account.model.AccountDeletionStatus;
import com.heddy.domain.account.port.out.AccountDeletionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountDeletionPersistenceAdapter implements AccountDeletionRepositoryPort {

    private final AccountDeletionRequestJpaRepository requestRepository;
    private final UsedReauthenticationTokenJpaRepository usedTokenRepository;

    @Override
    public AccountDeletionRequest save(AccountDeletionRequest request) {
        AccountDeletionRequestEntity entity = requestRepository.findById(request.deletionRequestId())
                .orElseGet(() -> new AccountDeletionRequestEntity(request));
        entity.update(request);
        return requestRepository.saveAndFlush(entity).toDomain();
    }

    @Override
    public Optional<AccountDeletionRequest> findProcessingByUserId(UUID userId) {
        return requestRepository.findFirstByUserIdAndStatusOrderByRequestedAtDesc(
                        userId, AccountDeletionStatus.PROCESSING)
                .map(AccountDeletionRequestEntity::toDomain);
    }

    @Override
    public List<AccountDeletionRequest> findProcessingBatch(int limit) {
        return requestRepository.findAllByStatusOrderByRequestedAtAsc(
                        AccountDeletionStatus.PROCESSING, PageRequest.of(0, limit)).stream()
                .map(AccountDeletionRequestEntity::toDomain)
                .toList();
    }

    @Override
    public boolean consumeReauthenticationToken(UUID tokenId, UUID userId, Instant usedAt) {
        try {
            usedTokenRepository.saveAndFlush(
                    new UsedReauthenticationTokenEntity(tokenId, userId, usedAt));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }
}
