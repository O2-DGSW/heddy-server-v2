package com.heddy.application.sharing.service;

import com.heddy.domain.account.port.out.SecureTokenGeneratorPort;
import com.heddy.domain.account.port.out.TokenHasherPort;
import com.heddy.domain.sharing.exception.SharingError;
import com.heddy.domain.sharing.exception.SharingException;
import com.heddy.domain.sharing.model.Share;
import com.heddy.domain.sharing.model.ShareFieldType;
import com.heddy.domain.sharing.model.SharePage;
import com.heddy.domain.sharing.model.ShareStatus;
import com.heddy.domain.sharing.port.in.CreateShareUseCase;
import com.heddy.domain.sharing.port.in.DeleteShareUseCase;
import com.heddy.domain.sharing.port.in.GetShareUseCase;
import com.heddy.domain.sharing.port.in.ListSharesUseCase;
import com.heddy.domain.sharing.port.in.UpdateShareUseCase;
import com.heddy.domain.sharing.port.out.ShareRepositoryPort;
import com.heddy.domain.style.model.SavedStyle;
import com.heddy.domain.style.port.out.SavedStyleRepositoryPort;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ShareServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String RAW_TOKEN = "raw-share-token";
    private static final String TOKEN_HASH = "hash-" + RAW_TOKEN;
    private static final String BASE_URL = "https://heddy.site/s";

    @Mock ShareRepositoryPort shareRepositoryPort;
    @Mock TreatmentRecordRepositoryPort treatmentRecordRepositoryPort;
    @Mock SavedStyleRepositoryPort savedStyleRepositoryPort;
    @Mock SecureTokenGeneratorPort tokenGeneratorPort;
    @Mock TokenHasherPort tokenHasherPort;

    private ShareService service;

    @BeforeEach
    void setUp() {
        service = new ShareService(shareRepositoryPort, treatmentRecordRepositoryPort,
                savedStyleRepositoryPort, tokenGeneratorPort, tokenHasherPort, BASE_URL);
    }

    // ------------------------------------------------------------------ 생성

    @Test
    void createsShareStoringOnlyTheHashAndReturnsUrlOnce() {
        UUID recordId = ownedRecord();
        stubToken();
        given(shareRepositoryPort.insert(any())).willAnswer(invocation -> invocation.getArgument(0));

        CreateShareUseCase.Result result = service.create(
                command(Set.of(recordId), Set.of(), fields(), null));

        ArgumentCaptor<Share> captor = ArgumentCaptor.forClass(Share.class);
        then(shareRepositoryPort).should().insert(captor.capture());
        assertThat(captor.getValue().tokenHash()).isEqualTo(TOKEN_HASH)
                .isNotEqualTo(RAW_TOKEN);
        assertThat(result.shareUrl()).isEqualTo(BASE_URL + "/" + RAW_TOKEN);
        assertThat(result.share().status()).isEqualTo(ShareStatus.ACTIVE);
        assertThat(result.share().expiresAt()).isAfter(Instant.now());
        assertThat(result.share().fields()).containsExactlyInAnyOrder(
                ShareFieldType.PHOTOS, ShareFieldType.TREATMENT_DETAILS);
    }

    @Test
    void defaultsExpiryToSevenDaysWhenOmitted() {
        UUID recordId = ownedRecord();
        stubToken();
        given(shareRepositoryPort.insert(any())).willAnswer(invocation -> invocation.getArgument(0));

        CreateShareUseCase.Result result = service.create(
                command(Set.of(recordId), Set.of(), fields(), null));

        assertThat(result.share().expiresAt())
                .isCloseTo(Instant.now().plusSeconds(7 * 86_400), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void acceptsOwnedSavedStylesWithoutAnyRecord() {
        UUID savedStyleId = UUID.randomUUID();
        stubToken();
        given(savedStyleRepositoryPort.findAllByUserIdAndIds(
                USER_ID, Set.of(savedStyleId)))
                .willReturn(List.of(savedStyle(savedStyleId)));
        given(shareRepositoryPort.insert(any())).willAnswer(invocation -> invocation.getArgument(0));

        CreateShareUseCase.Result result = service.create(command(
                Set.of(), Set.of(savedStyleId),
                Set.of(ShareFieldType.SAVED_STYLES), 3));

        assertThat(result.share().savedStyleIds()).containsExactly(savedStyleId);
        verifyNoInteractions(treatmentRecordRepositoryPort);
    }

    @Test
    void hidesForeignOrUnknownSavedStyleBehindResourceNotFound() {
        UUID foreignSavedStyleId = UUID.randomUUID();
        stubToken();
        given(savedStyleRepositoryPort.findAllByUserIdAndIds(
                USER_ID, Set.of(foreignSavedStyleId)))
                .willReturn(List.of());

        assertThatThrownBy(() -> service.create(command(
                Set.of(), Set.of(foreignSavedStyleId),
                Set.of(ShareFieldType.SAVED_STYLES), null)))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        then(shareRepositoryPort).shouldHaveNoInteractions();
    }

    @Test
    void hidesForeignRecordBehindResourceNotFound() {
        UUID foreignRecordId = UUID.randomUUID();
        stubToken();
        given(treatmentRecordRepositoryPort.findByIdAndUserId(foreignRecordId, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                command(Set.of(foreignRecordId), Set.of(), fields(), null)))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        then(shareRepositoryPort).shouldHaveNoInteractions();
    }

    @Test
    void propagatesEmptySelectionFromTheDomainWithoutTouchingStorage() {
        assertThatThrownBy(() -> service.create(command(Set.of(), Set.of(), fields(), null)))
                .isInstanceOfSatisfying(SharingException.class, e ->
                        assertThat(e.error()).isEqualTo(SharingError.EMPTY_SELECTION));
        // 토큰은 도메인 검증 전에 발급된다. 저장·소유권 질의는 일어나지 않는다.
        verifyNoInteractions(shareRepositoryPort, treatmentRecordRepositoryPort);
    }

    @Test
    void rejectsSelectionWithoutFieldsBeforeOwnershipQueries() {
        assertThatThrownBy(() -> service.create(
                command(Set.of(UUID.randomUUID()), Set.of(), Set.of(), null)))
                .isInstanceOfSatisfying(SharingException.class, e ->
                        assertThat(e.error()).isEqualTo(SharingError.EMPTY_SELECTION));
        then(treatmentRecordRepositoryPort).shouldHaveNoInteractions();
    }

    /**
     * 폐기가 반드시 저장보다 먼저다. 순서가 뒤집히면 같은 대상의 활성 링크가 잠깐 둘이 되고,
     * 부분 유니크 인덱스(V31)에 걸려 발급 자체가 실패한다.
     */
    @Test
    void closesThePreviousLinkBeforeInsertingTheNewOne() {
        UUID recordId = UUID.randomUUID();
        given(treatmentRecordRepositoryPort.findByIdAndUserId(recordId, USER_ID))
                .willReturn(Optional.of(mock(TreatmentRecord.class)));
        given(tokenGeneratorPort.generate()).willReturn("raw-token");
        given(tokenHasherPort.hash("raw-token")).willReturn("hashed");
        given(shareRepositoryPort.insert(any(Share.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        service.create(new CreateShareUseCase.Command(
                USER_ID, Set.of(recordId), Set.of(), Set.of(ShareFieldType.PHOTOS), 7));

        InOrder inOrder = inOrder(shareRepositoryPort);
        inOrder.verify(shareRepositoryPort).revokeActiveWithSameTarget(
                eq(USER_ID), eq(recordId + "|"), any(Instant.class));
        inOrder.verify(shareRepositoryPort).insert(any(Share.class));
    }

    // ------------------------------------------------------------------ 목록

    @Test
    void listsOwnSharesWithStatusFilterAndPagination() {
        List<Share> items = List.of(share(ShareStatus.REVOKED), share(ShareStatus.ACTIVE));
        given(shareRepositoryPort.findPage(eq(USER_ID), eq(ShareStatus.ACTIVE), eq(0), eq(20),
                any(Instant.class)))
                .willReturn(new SharePage(items, 2));

        ListSharesUseCase.Result result = service.list(
                new ListSharesUseCase.Query(USER_ID, ShareStatus.ACTIVE, 0, 20));

        assertThat(result.items()).hasSize(2);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void answersAnEmptyPageInsteadOfAnError() {
        given(shareRepositoryPort.findPage(eq(USER_ID), isNull(), eq(0), eq(20),
                any(Instant.class)))
                .willReturn(new SharePage(List.of(), 0));

        ListSharesUseCase.Result result = service.list(
                new ListSharesUseCase.Query(USER_ID, null, 0, 20));

        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void rejectsInvalidPagingAsBadRequest() {
        assertThatThrownBy(() -> service.list(
                        new ListSharesUseCase.Query(USER_ID, null, -1, 20)))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        assertThatThrownBy(() -> service.list(
                        new ListSharesUseCase.Query(USER_ID, null, 0, 101)))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        then(shareRepositoryPort).shouldHaveNoInteractions();
    }

    // ------------------------------------------------------------------ 상세·수정·철회

    @Test
    void getsOwnShareThroughTheOwnerScopedQuery() {
        Share stored = share(ShareStatus.ACTIVE);
        given(shareRepositoryPort.findByIdAndUserId(stored.shareId(), USER_ID))
                .willReturn(Optional.of(stored));

        assertThat(service.get(new GetShareUseCase.Query(USER_ID, stored.shareId())))
                .isSameAs(stored);
    }

    @Test
    void hidesForeignShareDuringGetUpdateAndDelete() {
        UUID shareId = UUID.randomUUID();
        given(shareRepositoryPort.findByIdAndUserId(shareId, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(new GetShareUseCase.Query(USER_ID, shareId)))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        assertThatThrownBy(() -> service.update(updateCommand(USER_ID, shareId)))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        assertThatThrownBy(() -> service.delete(
                        new DeleteShareUseCase.Command(USER_ID, shareId)))
                .isInstanceOfSatisfying(ApplicationException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        then(shareRepositoryPort).should(never()).update(any());
    }

    @Test
    void patchesOnlyPresentedFieldsKeepingTargetsUntouched() {
        Share current = share(ShareStatus.ACTIVE);
        given(shareRepositoryPort.findByIdAndUserId(current.shareId(), USER_ID))
                .willReturn(Optional.of(current));
        given(shareRepositoryPort.update(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        Instant newExpiresAt = Instant.now().plusSeconds(30 * 86_400);
        Share updated = service.update(new UpdateShareUseCase.Command(
                USER_ID, current.shareId(),
                UpdateShareUseCase.Patch.present(Set.of(ShareFieldType.MEMO)),
                UpdateShareUseCase.Patch.present(newExpiresAt)));

        assertThat(updated.fields()).containsExactly(ShareFieldType.MEMO);
        assertThat(updated.expiresAt()).isEqualTo(newExpiresAt);
        // 대상과 토큰은 수정 범위가 아니다.
        assertThat(updated.recordIds()).isEqualTo(current.recordIds());
        assertThat(updated.savedStyleIds()).isEqualTo(current.savedStyleIds());
        assertThat(updated.tokenHash()).isEqualTo(current.tokenHash());
    }

    @Test
    void patchWithoutAnyPresentedFieldKeepsEverything() {
        Share current = share(ShareStatus.ACTIVE);
        given(shareRepositoryPort.findByIdAndUserId(current.shareId(), USER_ID))
                .willReturn(Optional.of(current));
        given(shareRepositoryPort.update(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        Share updated = service.update(new UpdateShareUseCase.Command(
                USER_ID, current.shareId(),
                UpdateShareUseCase.Patch.absent(), UpdateShareUseCase.Patch.absent()));

        assertThat(updated).usingRecursiveComparison().isEqualTo(current);
    }

    @Test
    void refusesPatchToPastExpiryThroughTheDomain() {
        Share current = share(ShareStatus.ACTIVE);
        given(shareRepositoryPort.findByIdAndUserId(current.shareId(), USER_ID))
                .willReturn(Optional.of(current));

        assertThatThrownBy(() -> service.update(new UpdateShareUseCase.Command(
                USER_ID, current.shareId(),
                UpdateShareUseCase.Patch.absent(),
                UpdateShareUseCase.Patch.present(Instant.now().minusSeconds(60)))))
                .isInstanceOfSatisfying(SharingException.class, e ->
                        assertThat(e.error()).isEqualTo(SharingError.EXPIRES_AT_NOT_FUTURE));
        then(shareRepositoryPort).should(never()).update(any());
    }

    @Test
    void revokesInsteadOfDeletingTheRow() {
        Share current = share(ShareStatus.ACTIVE);
        given(shareRepositoryPort.findByIdAndUserId(current.shareId(), USER_ID))
                .willReturn(Optional.of(current));

        service.delete(new DeleteShareUseCase.Command(USER_ID, current.shareId()));

        ArgumentCaptor<Share> captor = ArgumentCaptor.forClass(Share.class);
        then(shareRepositoryPort).should().update(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ShareStatus.REVOKED);
        assertThat(captor.getValue().revokedAt()).isNotNull();
    }

    @Test
    void deletingAnAlreadyRevokedShareStaysQuiet() {
        // DB 에서 읽은 철회 행은 철회 시각을 이미 갖고 있다. 다시 DELETE 해도 그 값이 유지된다.
        Instant revokedAt = Instant.now().minusSeconds(60);
        Share revoked = Share.reconstitute(UUID.randomUUID(), USER_ID, TOKEN_HASH,
                ShareStatus.REVOKED, Instant.now().plusSeconds(86_400), revokedAt,
                Set.of(UUID.randomUUID()), Set.of(ShareFieldType.PHOTOS), Set.of(),
                Instant.now());
        given(shareRepositoryPort.findByIdAndUserId(revoked.shareId(), USER_ID))
                .willReturn(Optional.of(revoked));

        service.delete(new DeleteShareUseCase.Command(USER_ID, revoked.shareId()));

        ArgumentCaptor<Share> captor = ArgumentCaptor.forClass(Share.class);
        then(shareRepositoryPort).should().update(captor.capture());
        assertThat(captor.getValue().revokedAt()).isEqualTo(revokedAt);
    }

    // ------------------------------------------------------------------ 헬퍼

    private CreateShareUseCase.Command command(
            Set<UUID> recordIds, Set<UUID> savedStyleIds, Set<ShareFieldType> fields,
            Integer expiresInDays) {
        return new CreateShareUseCase.Command(
                USER_ID, recordIds, savedStyleIds, fields, expiresInDays);
    }

    private UpdateShareUseCase.Command updateCommand(UUID requesterId, UUID shareId) {
        return new UpdateShareUseCase.Command(requesterId, shareId,
                UpdateShareUseCase.Patch.absent(), UpdateShareUseCase.Patch.absent());
    }

    private Set<ShareFieldType> fields() {
        return Set.of(ShareFieldType.PHOTOS, ShareFieldType.TREATMENT_DETAILS);
    }

    private void stubToken() {
        given(tokenGeneratorPort.generate()).willReturn(RAW_TOKEN);
        given(tokenHasherPort.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
    }

    private UUID ownedRecord() {
        UUID recordId = UUID.randomUUID();
        given(treatmentRecordRepositoryPort.findByIdAndUserId(recordId, USER_ID))
                .willReturn(Optional.of(record(recordId)));
        return recordId;
    }

    private TreatmentRecord record(UUID recordId) {
        return new TreatmentRecord(recordId, USER_ID, Set.of(ServiceType.CUT), null, null,
                Instant.now().minusSeconds(60), null, null, null, null, null, null,
                List.of(), Instant.now());
    }

    private SavedStyle savedStyle(UUID savedStyleId) {
        return new SavedStyle(savedStyleId, USER_ID, "레이어드 커트",
                "https://images.example.com/layered.jpg", "추천 이유",
                null, null, null, null, Instant.now());
    }

    private Share share(ShareStatus status) {
        Instant now = Instant.now();
        return Share.reconstitute(UUID.randomUUID(), USER_ID, TOKEN_HASH, status,
                now.plusSeconds(86_400), null, Set.of(UUID.randomUUID()),
                Set.of(ShareFieldType.PHOTOS), Set.of(), now);
    }
}
