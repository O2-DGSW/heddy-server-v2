package com.heddy.adapter.out.persistence.sharing;

import com.heddy.domain.sharing.model.Share;
import com.heddy.domain.sharing.model.SharePage;
import com.heddy.domain.sharing.model.ShareStatus;
import com.heddy.domain.sharing.port.out.ShareRepositoryPort;
import com.heddy.domain.sharing.port.out.SharedRecordLookupPort;
import com.heddy.domain.account.port.out.TokenHasherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SharingPersistenceAdapter implements ShareRepositoryPort, SharedRecordLookupPort {

    private static final int MAX_PAGE_SIZE = 100;

    private final ShareJpaRepository shareRepository;
    /** 토큰과 같은 SHA-256 을 대상 구성에도 쓴다. 해시 계산이 한 곳이어야 값이 갈리지 않는다. */
    private final TokenHasherPort tokenHasherPort;

    @Override
    public Share insert(Share share) {
        return shareRepository
                .saveAndFlush(new ShareEntity(share, targetHash(share.targetKey())))
                .toDomain();
    }

    @Override
    public int revokeActiveWithSameTarget(UUID userId, String targetKey, Instant revokedAt) {
        return shareRepository.revokeActiveWithSameTarget(
                userId, targetHash(targetKey), revokedAt);
    }

    private String targetHash(String targetKey) {
        return tokenHasherPort.hash(targetKey);
    }

    @Override
    public Share update(Share share) {
        return shareRepository.findByShareIdAndUserId(share.shareId(), share.userId())
                .map(entity -> {
                    entity.updateFields(share.fields());
                    entity.updateExpiresAt(share.expiresAt());
                    entity.updateStatus(share.status().name(), share.revokedAt());
                    return shareRepository.saveAndFlush(entity);
                })
                .map(ShareEntity::toDomain)
                .orElseThrow(() -> new IllegalStateException("수정할 공유가 존재하지 않습니다."));
    }

    @Override
    public void detachSavedStyle(UUID savedStyleId) {
        shareRepository.deleteSavedStyleLinks(savedStyleId);
        shareRepository.flush();
    }

    @Override
    public Optional<Share> findByIdAndUserId(UUID shareId, UUID userId) {
        return shareRepository.findByShareIdAndUserId(shareId, userId).map(ShareEntity::toDomain);
    }

    @Override
    public Optional<Share> findByTokenHash(String tokenHash) {
        return shareRepository.findByTokenHash(tokenHash).map(ShareEntity::toDomain);
    }

    @Override
    public SharePage findPage(
            UUID userId, ShareStatus status, int page, int size, Instant now) {
        Page<ShareEntity> result = findEntityPage(userId, status, pageRequest(page, size), now);
        return new SharePage(
                result.getContent().stream().map(ShareEntity::toDomain).toList(),
                result.getTotalElements());
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        shareRepository.deleteAllByUserId(userId);
        shareRepository.flush();
    }

    @Override
    public Set<UUID> findSharedRecordIds(UUID ownerId, Collection<UUID> recordIds, Instant now) {
        // 빈 IN 절은 방언에 따라 문법 오류가 되고, 어차피 답이 정해져 있어 질의하지 않는다.
        if (recordIds.isEmpty()) {
            return Set.of();
        }
        return shareRepository.findSharedRecordIds(
                ownerId, recordIds, ShareStatus.ACTIVE.name(), now);
    }

    /**
     * ACTIVE 만 만료 조건이 붙는다. REVOKED 는 철회 시각이 이미 상태로 굳었고, 전체 조회는
     * 만료된 링크까지 보여 주는 것이 목록의 목적이라 그대로 둔다.
     */
    private Page<ShareEntity> findEntityPage(
            UUID userId, ShareStatus status, PageRequest pageRequest, Instant now) {
        if (status == null) {
            return shareRepository.findByUserId(userId, pageRequest);
        }
        if (status == ShareStatus.ACTIVE) {
            return shareRepository.findByUserIdAndStatusAndExpiresAtAfter(
                    userId, status.name(), now, pageRequest);
        }
        return shareRepository.findByUserIdAndStatus(userId, status.name(), pageRequest);
    }

    private PageRequest pageRequest(int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(
                Sort.Order.desc("createdAt"), Sort.Order.desc("shareId")));
    }
}
