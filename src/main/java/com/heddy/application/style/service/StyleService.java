package com.heddy.application.style.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.style.exception.StyleError;
import com.heddy.domain.style.exception.StyleException;
import com.heddy.domain.style.model.StyleTag;
import com.heddy.domain.style.model.StyleTagCategory;
import com.heddy.domain.style.model.UserStylePreference;
import com.heddy.domain.style.model.UserStylePreference.PreferenceType;
import com.heddy.domain.style.port.in.SaveStylePreferencesCommand;
import com.heddy.domain.style.port.in.StylePreferencesResult;
import com.heddy.domain.style.port.in.StyleUseCase;
import com.heddy.domain.style.port.out.StyleTagRepositoryPort;
import com.heddy.domain.style.port.out.UserStylePreferenceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleService implements StyleUseCase {

    private static final int MAX_TAGS_PER_TYPE = 10;

    private final AccountRepositoryPort accountRepositoryPort;
    private final StyleTagRepositoryPort styleTagRepositoryPort;
    private final UserStylePreferenceRepositoryPort preferenceRepositoryPort;

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
        validateNonDeletedAccount(command.userId());
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
        Account account = accountRepositoryPort.findById(userId)
                .orElseThrow(() -> new AccountException(AccountError.ACCOUNT_NOT_FOUND));
        if (account.isDeleted()) {
            throw new AccountException(AccountError.ACCOUNT_DELETED);
        }
    }
}
