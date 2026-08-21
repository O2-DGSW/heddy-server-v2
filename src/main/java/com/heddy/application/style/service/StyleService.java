package com.heddy.application.style.service;

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
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleService implements StyleUseCase {

    private static final int MAX_TAGS_PER_TYPE = 10;

    private final StyleTagRepositoryPort styleTagRepositoryPort;
    private final UserStylePreferenceRepositoryPort preferenceRepositoryPort;

    @Override
    public List<StyleTag> getStyleTags(StyleTagCategory category) {
        return styleTagRepositoryPort.findAll(category);
    }

    @Override
    public StylePreferencesResult getStylePreferences(UUID userId) {
        return StylePreferencesResult.from(preferenceRepositoryPort.findAllByUserId(userId));
    }

    @Override
    @Transactional
    public StylePreferencesResult saveStylePreferences(SaveStylePreferencesCommand command) {
        Set<UUID> preferredTagIds = new LinkedHashSet<>(command.preferredTagIds());
        Set<UUID> excludedTagIds = new LinkedHashSet<>(command.excludedTagIds());
        validateLimits(preferredTagIds, excludedTagIds);
        validateNoConflict(preferredTagIds, excludedTagIds);
        validateTagsExist(preferredTagIds, excludedTagIds);

        List<UserStylePreference> preferences = Stream.concat(
                        preferredTagIds.stream().map(tagId -> preference(
                                command.userId(), tagId, PreferenceType.PREFERRED)),
                        excludedTagIds.stream().map(tagId -> preference(
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
                .collect(java.util.stream.Collectors.toSet());
        if (!existingTagIds.containsAll(requestedTagIds)) {
            throw new StyleException(StyleError.TAG_NOT_FOUND);
        }
    }

    private UserStylePreference preference(UUID userId, UUID tagId, PreferenceType type) {
        return UserStylePreference.create(userId, tagId, type);
    }
}
