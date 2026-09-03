package com.heddy.application.style.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.style.exception.StyleError;
import com.heddy.domain.style.exception.StyleException;
import com.heddy.domain.style.model.StyleTag;
import com.heddy.domain.style.model.StyleTagCategory;
import com.heddy.domain.style.model.SavedStyle;
import com.heddy.domain.style.model.SavedStylePage;
import com.heddy.domain.style.model.UserStylePreference;
import com.heddy.domain.style.model.UserStylePreference.PreferenceType;
import com.heddy.domain.style.port.in.SaveStylePreferencesCommand;
import com.heddy.domain.style.port.in.SavedStyleUseCase;
import com.heddy.domain.style.port.in.StylePreferencesResult;
import com.heddy.domain.style.port.in.StyleUseCase;
import com.heddy.domain.style.port.out.SavedStyleRepositoryPort;
import com.heddy.domain.style.port.out.StyleTagRepositoryPort;
import com.heddy.domain.style.port.out.UserStylePreferenceRepositoryPort;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleService implements StyleUseCase, SavedStyleUseCase {

    private static final int MAX_TAGS_PER_TYPE = 10;
    private static final int MAX_SAVED_STYLES = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AccountRepositoryPort accountRepositoryPort;
    private final StyleTagRepositoryPort styleTagRepositoryPort;
    private final UserStylePreferenceRepositoryPort preferenceRepositoryPort;
    private final SavedStyleRepositoryPort savedStyleRepositoryPort;

    @Override
    public List<StyleTag> getStyleTags(StyleTagCategory category) {
        return styleTagRepositoryPort.findAll(category);
    }

    @Override
    public StylePreferencesResult getStylePreferences(UUID userId) {
        validateNonDeletedAccount(userId);
        return StylePreferencesResult.from(preferenceRepositoryPort.findAllByUserId(userId));
    }

    @Override
    @Transactional
    public StylePreferencesResult saveStylePreferences(SaveStylePreferencesCommand command) {
        lockAndValidateNonDeletedAccount(command.userId());
        Set<UUID> preferredTagIds = new LinkedHashSet<>(command.preferredTagIds());
        Set<UUID> excludedTagIds = new LinkedHashSet<>(command.excludedTagIds());
        validateLimits(preferredTagIds, excludedTagIds);
        validateNoConflict(preferredTagIds, excludedTagIds);
        validateTagsExist(preferredTagIds, excludedTagIds);

        List<UserStylePreference> preferences = Stream.concat(
                        preferredTagIds.stream().map(tagId -> UserStylePreference.create(
                                command.userId(), tagId, PreferenceType.PREFERRED)),
                        excludedTagIds.stream().map(tagId -> UserStylePreference.create(
                                command.userId(), tagId, PreferenceType.EXCLUDED)))
                .toList();
        return StylePreferencesResult.from(
                preferenceRepositoryPort.replace(command.userId(), preferences));
    }

    @Override
    @Transactional
    public SavedStyle create(SavedStyleUseCase.CreateCommand command) {
        lockAndValidateNonDeletedAccount(command.userId());
        String styleName = command.styleName().strip();
        String imageUrl = command.imageUrl().strip();
        if (savedStyleRepositoryPort.existsBySnapshot(command.userId(), styleName, imageUrl)) {
            throw new StyleException(StyleError.SAVED_STYLE_DUPLICATED);
        }
        if (savedStyleRepositoryPort.countByUserId(command.userId()) >= MAX_SAVED_STYLES) {
            throw new StyleException(StyleError.SAVED_STYLE_LIMIT_EXCEEDED);
        }
        return savedStyleRepositoryPort.insert(SavedStyle.create(
                command.userId(), styleName, imageUrl, command.reason(), command.memo()));
    }

    @Override
    public SavedStylePage list(SavedStyleUseCase.ListQuery query) {
        validateNonDeletedAccount(query.userId());
        if (query.page() < 0 || query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
        return savedStyleRepositoryPort.findPage(query.userId(), query.page(), query.size());
    }

    @Override
    @Transactional
    public SavedStyle updateMemo(SavedStyleUseCase.UpdateMemoCommand command) {
        validateNonDeletedAccount(command.userId());
        if (!command.memoPresent()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
        SavedStyle current = savedStyleRepositoryPort
                .findByIdAndUserId(command.savedStyleId(), command.userId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        return savedStyleRepositoryPort.update(current.updateMemo(command.memo()));
    }

    @Override
    @Transactional
    public void delete(SavedStyleUseCase.DeleteCommand command) {
        validateNonDeletedAccount(command.userId());
        if (!savedStyleRepositoryPort.deleteByIdAndUserId(
                command.savedStyleId(), command.userId())) {
            throw new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private void validateLimits(Set<UUID> preferredTagIds, Set<UUID> excludedTagIds) {
        if (preferredTagIds.size() > MAX_TAGS_PER_TYPE
                || excludedTagIds.size() > MAX_TAGS_PER_TYPE) {
            throw new StyleException(StyleError.PREFERENCE_LIMIT_EXCEEDED);
        }
    }

    private void validateNoConflict(Set<UUID> preferredTagIds, Set<UUID> excludedTagIds) {
        if (preferredTagIds.stream().anyMatch(excludedTagIds::contains)) {
            throw new StyleException(StyleError.PREFERENCE_CONFLICT);
        }
    }

    private void validateTagsExist(Set<UUID> preferredTagIds, Set<UUID> excludedTagIds) {
        Set<UUID> requestedTagIds = new LinkedHashSet<>();
        requestedTagIds.addAll(preferredTagIds);
        requestedTagIds.addAll(excludedTagIds);
        if (requestedTagIds.isEmpty()) {
            return;
        }

        Set<UUID> existingTagIds = styleTagRepositoryPort.findAllByIds(requestedTagIds).stream()
                .map(StyleTag::styleTagId)
                .collect(Collectors.toSet());
        List<UUID> invalidPreferredTagIds = preferredTagIds.stream()
                .filter(tagId -> !existingTagIds.contains(tagId))
                .toList();
        List<UUID> invalidExcludedTagIds = excludedTagIds.stream()
                .filter(tagId -> !existingTagIds.contains(tagId))
                .toList();
        if (!invalidPreferredTagIds.isEmpty() || !invalidExcludedTagIds.isEmpty()) {
            throw StyleException.invalidTagIds(
                    invalidPreferredTagIds, invalidExcludedTagIds);
        }
    }

    private void validateNonDeletedAccount(UUID userId) {
        validateAccount(accountRepositoryPort.findById(userId));
    }

    private void lockAndValidateNonDeletedAccount(UUID userId) {
        validateAccount(accountRepositoryPort.findByIdForUpdate(userId));
    }

    private void validateAccount(Optional<Account> accountResult) {
        Account account = accountResult
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND));
        if (account.isDeleted()) {
            throw new AccountException(AccountError.ACCOUNT_DELETED);
        }
    }
}
