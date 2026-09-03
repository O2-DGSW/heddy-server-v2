package com.heddy.adapter.out.persistence.style;

import com.heddy.domain.style.model.SavedStyle;
import com.heddy.domain.style.model.SavedStylePage;
import com.heddy.domain.style.port.out.SavedStyleRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SavedStylePersistenceAdapter implements SavedStyleRepositoryPort {

    private final SavedStyleJpaRepository repository;

    @Override
    public SavedStyle insert(SavedStyle savedStyle) {
        return repository.saveAndFlush(new SavedStyleEntity(savedStyle)).toDomain();
    }

    @Override
    public List<SavedStyle> findAllByUserIdAndIds(
            UUID userId,
            Collection<UUID> savedStyleIds
    ) {
        if (savedStyleIds.isEmpty()) {
            return List.of();
        }
        return repository
                .findAllByUserIdAndSavedStyleIdInOrderByCreatedAtDescSavedStyleIdDesc(
                        userId, savedStyleIds)
                .stream()
                .map(SavedStyleEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<SavedStyle> findByIdAndUserId(UUID savedStyleId, UUID userId) {
        return repository.findBySavedStyleIdAndUserId(savedStyleId, userId)
                .map(SavedStyleEntity::toDomain);
    }

    @Override
    public SavedStylePage findPage(UUID userId, int page, int size) {
        var result = repository.findAllByUserId(userId, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("savedStyleId"))));
        return new SavedStylePage(
                result.getContent().stream().map(SavedStyleEntity::toDomain).toList(),
                result.getTotalElements());
    }

    @Override
    public long countByUserId(UUID userId) {
        return repository.countByUserId(userId);
    }

    @Override
    public boolean existsBySnapshot(UUID userId, String styleName, String imageUrl) {
        return repository.existsByUserIdAndStyleNameAndImageUrl(userId, styleName, imageUrl);
    }

    @Override
    public SavedStyle update(SavedStyle savedStyle) {
        SavedStyleEntity entity = repository
                .findBySavedStyleIdAndUserId(savedStyle.savedStyleId(), savedStyle.userId())
                .orElseThrow(() -> new IllegalStateException("수정할 저장 후보가 존재하지 않습니다."));
        entity.updateMemo(savedStyle.memo());
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    public boolean deleteByIdAndUserId(UUID savedStyleId, UUID userId) {
        if (repository.findBySavedStyleIdAndUserId(savedStyleId, userId).isEmpty()) {
            return false;
        }
        repository.deleteShareLinks(savedStyleId);
        boolean deleted = repository.deleteBySavedStyleIdAndUserId(savedStyleId, userId) == 1;
        repository.flush();
        return deleted;
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        repository.deleteAllByUserId(userId);
        repository.flush();
    }
}
