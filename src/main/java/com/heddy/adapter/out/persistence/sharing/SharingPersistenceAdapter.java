package com.heddy.adapter.out.persistence.sharing;

import com.heddy.domain.sharing.model.Share;
import com.heddy.domain.sharing.model.SharePage;
import com.heddy.domain.sharing.model.ShareStatus;
import com.heddy.domain.sharing.port.out.ShareRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SharingPersistenceAdapter implements ShareRepositoryPort {

    private static final int MAX_PAGE_SIZE = 100;

    private final ShareJpaRepository shareRepository;

    @Override
    public Share insert(Share share) {
        return shareRepository.saveAndFlush(new ShareEntity(share)).toDomain();
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
    public Optional<Share> findByIdAndUserId(UUID shareId, UUID userId) {
        return shareRepository.findByShareIdAndUserId(shareId, userId).map(ShareEntity::toDomain);
    }

    @Override
    public Optional<Share> findByTokenHash(String tokenHash) {
        return shareRepository.findByTokenHash(tokenHash).map(ShareEntity::toDomain);
    }

    @Override
    public SharePage findPage(UUID userId, ShareStatus status, int page, int size) {
        Page<ShareEntity> result = status == null
                ? shareRepository.findByUserId(userId, pageRequest(page, size))
                : shareRepository.findByUserIdAndStatus(userId, status.name(),
                        pageRequest(page, size));
        return new SharePage(
                result.getContent().stream().map(ShareEntity::toDomain).toList(),
                result.getTotalElements());
    }

    private PageRequest pageRequest(int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(
                Sort.Order.desc("createdAt"), Sort.Order.desc("shareId")));
    }
}
