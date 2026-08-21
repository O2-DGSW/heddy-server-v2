package com.heddy.application.style.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.style.exception.StyleError;
import com.heddy.domain.style.exception.StyleException;
import com.heddy.domain.style.model.StyleTag;
import com.heddy.domain.style.model.StyleTagCategory;
import com.heddy.domain.style.model.UserStylePreference;
import com.heddy.domain.style.port.in.SaveStylePreferencesCommand;
import com.heddy.domain.style.port.out.StyleTagRepositoryPort;
import com.heddy.domain.style.port.out.UserStylePreferenceRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StyleServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock AccountRepositoryPort accountRepositoryPort;
    @Mock StyleTagRepositoryPort styleTagRepositoryPort;
    @Mock UserStylePreferenceRepositoryPort preferenceRepositoryPort;

    private StyleService service;

    @BeforeEach
    void setUp() {
        service = new StyleService(
                accountRepositoryPort, styleTagRepositoryPort, preferenceRepositoryPort);
    }

    @Test
    void getsStyleTagsByCategory() {
        StyleTag tag = tag(UUID.randomUUID(), StyleTagCategory.BANG);
        given(styleTagRepositoryPort.findAll(StyleTagCategory.BANG))
                .willReturn(List.of(tag));

        assertThat(service.getStyleTags(StyleTagCategory.BANG)).containsExactly(tag);
    }

    @Test
    void getsAllStyleTagsWhenCategoryIsNotSpecified() {
        StyleTag tag = tag(UUID.randomUUID(), StyleTagCategory.LONG);
        given(styleTagRepositoryPort.findAll(null)).willReturn(List.of(tag));

        assertThat(service.getStyleTags(null)).containsExactly(tag);
    }

    @Test
    void getsCurrentUsersPreferredAndExcludedTags() {
        givenActiveAccount();
        UUID preferredTagId = UUID.randomUUID();
        UUID excludedTagId = UUID.randomUUID();
        given(preferenceRepositoryPort.findAllByUserId(USER_ID)).willReturn(List.of(
                preference(preferredTagId, UserStylePreference.PreferenceType.PREFERRED),
                preference(excludedTagId, UserStylePreference.PreferenceType.EXCLUDED)));

        var result = service.getStylePreferences(USER_ID);

        assertThat(result.preferredTagIds()).containsExactly(preferredTagId);
        assertThat(result.excludedTagIds()).containsExactly(excludedTagId);
    }

    @Test
    void replacesAllPreferencesAndRemovesDuplicateIdsWithinAList() {
        givenActiveAccount();
        UUID preferredTagId = UUID.randomUUID();
        UUID excludedTagId = UUID.randomUUID();
        given(styleTagRepositoryPort.findAllByIds(any())).willReturn(List.of(
                tag(preferredTagId, StyleTagCategory.BANG),
                tag(excludedTagId, StyleTagCategory.LONG)));
        given(preferenceRepositoryPort.replace(any(), any()))
                .willAnswer(invocation -> invocation.getArgument(1));

        var result = service.saveStylePreferences(new SaveStylePreferencesCommand(
                USER_ID,
                List.of(preferredTagId, preferredTagId),
                List.of(excludedTagId)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserStylePreference>> captor = ArgumentCaptor.forClass(List.class);
        verify(preferenceRepositoryPort).replace(eq(USER_ID), captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(result.preferredTagIds()).containsExactly(preferredTagId);
        assertThat(result.excludedTagIds()).containsExactly(excludedTagId);
    }

    @Test
    void rejectsMoreThanTenTagsForEitherType() {
        givenActiveAccount();
        List<UUID> elevenTagIds = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            elevenTagIds.add(UUID.randomUUID());
        }

        assertError(() -> service.saveStylePreferences(new SaveStylePreferencesCommand(
                        USER_ID, elevenTagIds, List.of())),
                StyleError.PREFERENCE_LIMIT_EXCEEDED);
        verify(preferenceRepositoryPort, never()).replace(any(), any());
    }

    @Test
    void rejectsTagIncludedInBothLists() {
        givenActiveAccount();
        UUID tagId = UUID.randomUUID();

        assertError(() -> service.saveStylePreferences(new SaveStylePreferencesCommand(
                        USER_ID, List.of(tagId), List.of(tagId))),
                StyleError.PREFERENCE_CONFLICT);
        verify(styleTagRepositoryPort, never()).findAllByIds(any());
    }

    @Test
    void rejectsUnknownTagBeforeReplacingExistingPreferences() {
        givenActiveAccount();
        UUID unknownTagId = UUID.randomUUID();
        given(styleTagRepositoryPort.findAllByIds(any())).willReturn(List.of());

        assertThatThrownBy(() -> service.saveStylePreferences(
                new SaveStylePreferencesCommand(
                        USER_ID, List.of(unknownTagId), List.of())))
                .isInstanceOfSatisfying(StyleException.class, exception -> {
                    assertThat(exception.error()).isEqualTo(StyleError.INVALID_TAG_IDS);
                    assertThat(exception.invalidPreferredTagIds())
                            .containsExactly(unknownTagId);
                    assertThat(exception.invalidExcludedTagIds()).isEmpty();
                });
        verify(preferenceRepositoryPort, never()).replace(any(), any());
    }

    @Test
    void rejectsDeletedAccountsBeforeLoadingPreferences() {
        given(accountRepositoryPort.findById(USER_ID))
                .willReturn(Optional.of(account(AccountStatus.DELETED)));

        assertAccountError(() -> service.getStylePreferences(USER_ID),
                AccountError.ACCOUNT_DELETED);
        verifyNoInteractions(preferenceRepositoryPort);
    }

    @Test
    void rejectsDeletionPendingAccountsBeforeSavingPreferences() {
        given(accountRepositoryPort.findById(USER_ID))
                .willReturn(Optional.of(account(AccountStatus.DELETION_PENDING)));

        assertAccountError(() -> service.saveStylePreferences(
                        new SaveStylePreferencesCommand(USER_ID, List.of(), List.of())),
                AccountError.ACCOUNT_DELETED);
        verifyNoInteractions(styleTagRepositoryPort, preferenceRepositoryPort);
    }

    private StyleTag tag(UUID tagId, StyleTagCategory category) {
        return new StyleTag(tagId, "태그", category);
    }

    private UserStylePreference preference(
            UUID tagId,
            UserStylePreference.PreferenceType type
    ) {
        return UserStylePreference.create(USER_ID, tagId, type);
    }

    private void givenActiveAccount() {
        given(accountRepositoryPort.findById(USER_ID))
                .willReturn(Optional.of(account(AccountStatus.ACTIVE)));
    }

    private Account account(AccountStatus status) {
        return new Account(USER_ID, "style-user@example.com", "hash",
                AuthProvider.EMAIL, null, status, 0, null);
    }

    private void assertError(Runnable action, StyleError expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(StyleException.class,
                        exception -> assertThat(exception.error()).isEqualTo(expected));
    }

    private void assertAccountError(Runnable action, AccountError expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(AccountException.class,
                        exception -> assertThat(exception.error()).isEqualTo(expected));
    }
}
