package com.heddy.adapter.out.persistence.style;

import com.heddy.domain.style.model.SavedStyle;
import com.heddy.domain.style.port.out.SavedStyleRepositoryPort;
import lombok.RequiredArgsConstructor;
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
    public SavedStyle update(SavedStyle savedStyle) {
        SavedStyleEntity entity = repository
                .findBySavedStyleIdAndUserId(savedStyle.savedStyleId(), savedStyle.userId())
                .orElseThrow(() -> new IllegalStateException("수정할 저장 후보가 존재하지 않습니다."));
        entity.updateMemo(savedStyle.memo());
        return repository.saveAndFlush(entity).toDomain();
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
    public List<SavedStyle> findAllByUserId(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDescSavedStyleIdDesc(userId).stream()
                .map(SavedStyleEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<SavedStyle> findByIdAndUserId(UUID savedStyleId, UUID userId) {
        return repository.findBySavedStyleIdAndUserId(savedStyleId, userId)
                .map(SavedStyleEntity::toDomain);
    }

    @Override
    public boolean deleteById(UUID savedStyleId) {
        boolean deleted = repository.deleteBySavedStyleId(savedStyleId) == 1;
        if (deleted) {
            repository.flush();
        }
        return deleted;
    }

    @Override
    public List<UUID> findHairstyleIdsByUserId(UUID userId) {
        return repository.findHairstyleIdsByUserId(userId);
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        repository.deleteAllByUserId(userId);
        repository.flush();
    }
}
