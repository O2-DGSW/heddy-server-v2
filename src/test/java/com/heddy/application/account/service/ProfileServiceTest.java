package com.heddy.application.account.service;

import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AccountStatus;
import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.account.model.HairProfile.HairCondition;
import com.heddy.domain.account.model.HairProfile.HairLength;
import com.heddy.domain.account.model.HairProfile.HairThickness;
import com.heddy.domain.account.model.HairProfile.HairType;
import com.heddy.domain.account.model.UserProfile;
import com.heddy.domain.account.port.in.SaveHairProfileCommand;
import com.heddy.domain.account.port.in.UpdateMyProfileCommand;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.HairProfileRepositoryPort;
import com.heddy.domain.account.port.out.UserProfileRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-08-18T05:30:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-08-19T05:30:00Z");

    @Mock AccountRepositoryPort accountRepositoryPort;
    @Mock UserProfileRepositoryPort userProfileRepositoryPort;
    @Mock HairProfileRepositoryPort hairProfileRepositoryPort;

    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(accountRepositoryPort, userProfileRepositoryPort,
                hairProfileRepositoryPort);
    }

    @Test
    void getsCurrentUsersProfile() {
        given(accountRepositoryPort.findById(USER_ID)).willReturn(Optional.of(account()));
        given(userProfileRepositoryPort.findByUserId(USER_ID)).willReturn(Optional.of(profile()));

        var result = service.getProfile(USER_ID);

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.email()).isEqualTo("user@example.com");
        assertThat(result.nickname()).isEqualTo("헤디");
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void partialUpdateKeepsOmittedFieldsAndClearsExplicitNull() {
        given(accountRepositoryPort.findById(USER_ID)).willReturn(Optional.of(account()));
        given(userProfileRepositoryPort.findByUserId(USER_ID)).willReturn(Optional.of(profile()));
        given(userProfileRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.updateProfile(new UpdateMyProfileCommand(
                USER_ID, true, "새 닉네임", true, null,
                false, null, false, null));

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().nickname()).isEqualTo("새 닉네임");
        assertThat(captor.getValue().phone()).isNull();
        assertThat(captor.getValue().preferredDesigner()).isEqualTo("김디자이너");
        assertThat(captor.getValue().hairCautions()).isEqualTo("두피 자극 주의");
    }

    @Test
    void rejectsBlankNickname() {
        given(accountRepositoryPort.findById(USER_ID)).willReturn(Optional.of(account()));
        given(userProfileRepositoryPort.findByUserId(USER_ID)).willReturn(Optional.of(profile()));

        assertError(() -> service.updateProfile(new UpdateMyProfileCommand(
                USER_ID, true, " ", false, null,
                false, null, false, null)), AccountError.PROFILE_INVALID_NICKNAME);
        verify(userProfileRepositoryPort, never()).save(any());
    }

    @Test
    void rejectsPhoneOwnedByAnotherUser() {
        given(accountRepositoryPort.findById(USER_ID)).willReturn(Optional.of(account()));
        given(userProfileRepositoryPort.findByUserId(USER_ID)).willReturn(Optional.of(profile()));
        given(userProfileRepositoryPort.findUserIdByPhone("01099998888"))
                .willReturn(Optional.of(UUID.randomUUID()));

        assertError(() -> service.updateProfile(new UpdateMyProfileCommand(
                USER_ID, false, null, true, "01099998888",
                false, null, false, null)), AccountError.PHONE_ALREADY_EXISTS);
    }

    @Test
    void returnsDocumentedErrorWhenHairProfileDoesNotExist() {
        given(hairProfileRepositoryPort.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertError(() -> service.getHairProfile(USER_ID), AccountError.HAIR_PROFILE_NOT_FOUND);
    }

    @Test
    void createsHairProfileWhenItDoesNotExist() {
        given(accountRepositoryPort.findById(USER_ID)).willReturn(Optional.of(account()));
        given(hairProfileRepositoryPort.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(hairProfileRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        HairProfile result = service.saveHairProfile(hairCommand());

        assertThat(result.hairProfileId()).isNotNull();
        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.hairType()).isEqualTo(HairType.WAVY);
        assertThat(result.availableCareTimeMinutes()).isEqualTo(15);
    }

    @Test
    void replacesExistingHairProfileWithoutChangingItsId() {
        UUID hairProfileId = UUID.randomUUID();
        HairProfile current = new HairProfile(hairProfileId, USER_ID, HairType.STRAIGHT,
                HairCondition.HEALTHY, HairLength.SHORT, HairThickness.THIN, 5,
                CREATED_AT, UPDATED_AT);
        given(accountRepositoryPort.findById(USER_ID)).willReturn(Optional.of(account()));
        given(hairProfileRepositoryPort.findByUserId(USER_ID)).willReturn(Optional.of(current));
        given(hairProfileRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        HairProfile result = service.saveHairProfile(hairCommand());

        assertThat(result.hairProfileId()).isEqualTo(hairProfileId);
        assertThat(result.hairCondition()).isEqualTo(HairCondition.NORMAL);
    }

    private Account account() {
        return new Account(USER_ID, "user@example.com", "hash", AuthProvider.EMAIL, null,
                AccountStatus.ACTIVE, 0, null, CREATED_AT, UPDATED_AT);
    }

    private UserProfile profile() {
        return new UserProfile(USER_ID, "헤디", "01012345678", "김디자이너",
                "두피 자극 주의", CREATED_AT, UPDATED_AT);
    }

    private SaveHairProfileCommand hairCommand() {
        return new SaveHairProfileCommand(USER_ID, HairType.WAVY, HairCondition.NORMAL,
                HairLength.BELOW_SHOULDER, HairThickness.THICK, 15);
    }

    private void assertError(Runnable action, AccountError expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(AccountException.class,
                        exception -> assertThat(exception.error()).isEqualTo(expected));
    }
}
