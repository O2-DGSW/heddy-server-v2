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
import com.heddy.domain.sharing.port.in.ListSharesUseCase;
import com.heddy.domain.sharing.port.out.ShareRepositoryPort;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ShareServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String RAW_TOKEN = "raw-share-token";
    private static final String TOKEN_HASH = "hash-" + RAW_TOKEN;
    private static final String BASE_URL = "https://heddy.example.com/s";

    @Mock ShareRepositoryPort shareRepositoryPort;
    @Mock TreatmentRecordRepositoryPort treatmentRecordRepositoryPort;
    @Mock SecureTokenGeneratorPort tokenGeneratorPort;
    @Mock TokenHasherPort tokenHasherPort;

    private ShareService service;

    @BeforeEach
    void setUp() {
        service = new ShareService(shareRepositoryPort, treatmentRecordRepositoryPort,
                tokenGeneratorPort, tokenHasherPort, BASE_URL);
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
    void acceptsSavedStylesWithoutAnyRecord() {
        stubToken();
        given(shareRepositoryPort.insert(any())).willAnswer(invocation -> invocation.getArgument(0));

        CreateShareUseCase.Result result = service.create(command(
                Set.of(), Set.of(UUID.randomUUID()),
                Set.of(ShareFieldType.SAVED_STYLES), 3));

        assertThat(result.share().savedStyleIds()).hasSize(1);
        // 후보 스타일 도메인이 아직 없어 기록 소유권 질의는 일어나지 않는다.
        verifyNoInteractions(treatmentRecordRepositoryPort);
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

    // ------------------------------------------------------------------ 목록

    @Test
    void listsOwnSharesWithStatusFilterAndPagination() {
        List<Share> items = List.of(share(ShareStatus.REVOKED), share(ShareStatus.ACTIVE));
        given(shareRepositoryPort.findPage(USER_ID, ShareStatus.ACTIVE, 0, 20))
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
        given(shareRepositoryPort.findPage(USER_ID, null, 0, 20))
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

    // ------------------------------------------------------------------ 헬퍼

    private CreateShareUseCase.Command command(
            Set<UUID> recordIds, Set<UUID> savedStyleIds, Set<ShareFieldType> fields,
            Integer expiresInDays) {
        return new CreateShareUseCase.Command(
                USER_ID, recordIds, savedStyleIds, fields, expiresInDays);
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

    private Share share(ShareStatus status) {
        Instant now = Instant.now();
        return Share.reconstitute(UUID.randomUUID(), USER_ID, TOKEN_HASH, status,
                now.plusSeconds(86_400), null, Set.of(UUID.randomUUID()),
                Set.of(ShareFieldType.PHOTOS), Set.of(), now);
    }
}
