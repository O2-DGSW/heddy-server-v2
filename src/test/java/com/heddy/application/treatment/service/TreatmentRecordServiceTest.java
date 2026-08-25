package com.heddy.application.treatment.service;

import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.treatment.exception.TreatmentError;
import com.heddy.domain.treatment.exception.TreatmentException;
import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.model.TreatmentPhoto;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.model.TreatmentRecordFilter;
import com.heddy.domain.treatment.model.TreatmentRecordPage;
import com.heddy.domain.treatment.port.in.CreateTreatmentRecordUseCase.Command;
import com.heddy.domain.treatment.port.in.GetTreatmentRecordUseCase.Query;
import com.heddy.domain.treatment.port.in.ListTreatmentRecordsUseCase;
import com.heddy.domain.treatment.port.in.DeleteTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.UpdateTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TreatmentRecordServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant PERFORMED_AT = Instant.now().minusSeconds(60);

    @Mock TreatmentRecordRepositoryPort recordRepositoryPort;
    @Mock FileRepositoryPort fileRepositoryPort;
    @Mock FileStoragePort fileStoragePort;

    private TreatmentRecordService service;

    @BeforeEach
    void setUp() {
        service = new TreatmentRecordService(
                recordRepositoryPort, fileRepositoryPort, fileStoragePort);
    }

    // ------------------------------------------------------------------ 등록

    @Test
    void createsRecordWithoutPhotosThroughTheDomainFactory() {
        given(recordRepositoryPort.insert(any())).willAnswer(invocation -> invocation.getArgument(0));

        TreatmentRecord saved = service.create(command(PERFORMED_AT, List.of()));

        assertThat(saved.recordId()).isNotNull();
        assertThat(saved.userId()).isEqualTo(USER_ID);
        assertThat(saved.serviceTypes()).containsExactlyInAnyOrder(ServiceType.CUT, ServiceType.COLOR);
        assertThat(saved.photos()).isEmpty();
        verify(recordRepositoryPort).insert(saved);
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void attachesReadyOwnedPhotoFilesWithoutIssuingAnyUrlAtCreationTime() {
        UUID fileId = UUID.randomUUID();
        given(fileRepositoryPort.findById(fileId))
                .willReturn(Optional.of(readyFile(fileId)));
        given(recordRepositoryPort.insert(any())).willAnswer(invocation -> invocation.getArgument(0));

        TreatmentRecord saved = service.create(command(PERFORMED_AT,
                List.of(new Command.Photo(fileId, ImageType.BEFORE))));

        assertThat(saved.photos()).hasSize(1);
        assertThat(saved.photos().get(0).fileId()).isEqualTo(fileId);
        assertThat(saved.photos().get(0).imageType()).isEqualTo(ImageType.BEFORE);
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void rejectsFuturePerformedAtBeforeTouchingTheRepository() {
        Instant tomorrow = Instant.now().plusSeconds(86_400);

        assertThatThrownBy(() -> service.create(command(tomorrow, List.of())))
                .isInstanceOf(TreatmentException.class)
                .hasFieldOrPropertyWithValue("error", TreatmentError.PERFORMED_AT_IN_FUTURE);
        verifyNoInteractions(recordRepositoryPort);
    }

    @Test
    void rejectsEmptyServiceTypesBeforeTouchingTheRepository() {
        Command empty = new Command(USER_ID, Set.of(), null, null, PERFORMED_AT,
                null, null, null, null, List.of());

        assertThatThrownBy(() -> service.create(empty))
                .isInstanceOf(TreatmentException.class)
                .hasFieldOrPropertyWithValue("error", TreatmentError.SERVICE_TYPE_REQUIRED);
        verifyNoInteractions(recordRepositoryPort);
    }

    @Test
    void refusesPhotoFileOwnedBySomeoneElse() {
        UUID foreignFileId = UUID.randomUUID();
        given(fileRepositoryPort.findById(foreignFileId))
                .willReturn(Optional.of(readyFileOwnedBy(foreignFileId, UUID.randomUUID())));

        assertCreateRejected(foreignFileId, ErrorCode.FORBIDDEN_RESOURCE);
    }

    @Test
    void refusesPhotoFileThatHasNotFinishedUploading() {
        UUID pendingFileId = UUID.randomUUID();
        StoredFile pending = new StoredFile(pendingFileId, UUID.randomUUID(), USER_ID,
                FilePurpose.TREATMENT_PHOTO, FileStatus.PENDING, "TREATMENT_PHOTO/k",
                "image/jpeg", "a.jpg", 10, null, null, null,
                Instant.now().plusSeconds(300), Instant.now());
        given(fileRepositoryPort.findById(pendingFileId)).willReturn(Optional.of(pending));

        assertCreateRejected(pendingFileId, ErrorCode.FILE_INVALID_STATE);
    }

    @Test
    void refusesUnknownPhotoFile() {
        UUID unknownFileId = UUID.randomUUID();
        given(fileRepositoryPort.findById(unknownFileId)).willReturn(Optional.empty());

        assertCreateRejected(unknownFileId, ErrorCode.RESOURCE_NOT_FOUND);
    }

    private void assertCreateRejected(UUID fileId, ErrorCode expected) {
        assertThatThrownBy(() -> service.create(command(PERFORMED_AT,
                List.of(new Command.Photo(fileId, ImageType.AFTER)))))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", expected);
        verify(recordRepositoryPort, never()).insert(any());
    }

    // ------------------------------------------------------------------ 조회

    @Test
    void loadsOwnRecordAndSignsFreshDownloadUrlsPerPhoto() {
        UUID recordId = UUID.randomUUID();
        // 사진의 record_id 는 기록 식별자와 같아야 한다(도메인 불변식).
        TreatmentPhoto photo = new TreatmentPhoto(UUID.randomUUID(), recordId,
                UUID.randomUUID(), ImageType.AFTER, Instant.now());
        TreatmentRecord record = new TreatmentRecord(recordId, USER_ID,
                Set.of(ServiceType.CUT), null, null, PERFORMED_AT, null, null, null, null,
                List.of(photo), Instant.now());
        URI signed = URI.create("https://bucket.s3.example/signed?X-Amz-Signature=s");
        given(recordRepositoryPort.findByIdAndUserId(record.recordId(), USER_ID))
                .willReturn(Optional.of(record));
        given(fileRepositoryPort.findById(photo.fileId()))
                .willReturn(Optional.of(readyFile(photo.fileId())));
        given(fileStoragePort.createDownloadUrl(any())).willReturn(signed);

        var result = service.get(new Query(USER_ID, record.recordId()));

        assertThat(result.record().recordId()).isEqualTo(record.recordId());
        assertThat(result.photoUrls()).containsEntry(photo.photoId(), signed);
        verify(fileStoragePort).createDownloadUrl(any());
    }

    /**
     * 남의 기록은 없는 기록과 똑같이 RESOURCE_NOT_FOUND 다. 존재 여부를 노출하지 않는다(#31).
     * 소유자 조건이 조회에 실려 나가므로 사진·파일은 아예 읽히지 않는다.
     */
    @Test
    void answersResourceNotFoundForAnotherUsersRecord() {
        UUID recordId = UUID.randomUUID();
        given(recordRepositoryPort.findByIdAndUserId(recordId, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(new Query(USER_ID, recordId)))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
        verifyNoInteractions(fileRepositoryPort);
        verifyNoInteractions(fileStoragePort);
    }

    /** 소유권을 메모리에서 거르지 않는다 — 요청자 식별자가 조회 조건으로 그대로 넘어가야 한다. */
    @Test
    void asksTheRepositoryForTheRecordScopedToTheRequester() {
        UUID recordId = UUID.randomUUID();
        TreatmentRecord record = new TreatmentRecord(recordId, USER_ID,
                Set.of(ServiceType.CUT), null, null, PERFORMED_AT, null, null, null, null,
                List.of(), Instant.now());
        given(recordRepositoryPort.findByIdAndUserId(recordId, USER_ID))
                .willReturn(Optional.of(record));

        service.get(new Query(USER_ID, recordId));

        verify(recordRepositoryPort).findByIdAndUserId(recordId, USER_ID);
    }

    /** 파일이 READY 가 아니면 URL 자리를 비워 둔다 — 사진 자체는 응답에서 사라지지 않는다. */
    @Test
    void leavesTheUrlEmptyForAPhotoWhoseFileIsNotReady() {
        UUID recordId = UUID.randomUUID();
        TreatmentPhoto photo = new TreatmentPhoto(UUID.randomUUID(), recordId,
                UUID.randomUUID(), ImageType.AFTER, Instant.now());
        TreatmentRecord record = new TreatmentRecord(recordId, USER_ID,
                Set.of(ServiceType.CUT), null, null, PERFORMED_AT, null, null, null, null,
                List.of(photo), Instant.now());
        given(recordRepositoryPort.findByIdAndUserId(recordId, USER_ID))
                .willReturn(Optional.of(record));
        given(fileRepositoryPort.findById(photo.fileId()))
                .willReturn(Optional.of(fileInStatus(photo.fileId(), FileStatus.DELETED)));

        var result = service.get(new Query(USER_ID, recordId));

        assertThat(result.photoUrls()).containsEntry(photo.photoId(), null);
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void answersResourceNotFoundForUnknownRecordId() {
        given(recordRepositoryPort.findByIdAndUserId(any(), any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(new Query(USER_ID, UUID.randomUUID())))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ------------------------------------------------------------------ 목록

    @Test
    void listsFilteredRecordsAndSignsTheFirstAvailableThumbnail() {
        UUID recordId = UUID.randomUUID();
        TreatmentPhoto photo = new TreatmentPhoto(UUID.randomUUID(), recordId,
                UUID.randomUUID(), ImageType.AFTER, Instant.now());
        TreatmentRecord record = new TreatmentRecord(recordId, USER_ID,
                Set.of(ServiceType.CUT), "준헤어", "김실장", PERFORMED_AT, 5,
                null, null, null, List.of(photo), Instant.now());
        given(recordRepositoryPort.findPage(any()))
                .willReturn(new TreatmentRecordPage(List.of(record), 3));
        given(fileRepositoryPort.findById(photo.fileId()))
                .willReturn(Optional.of(readyFile(photo.fileId())));
        URI signed = URI.create("https://bucket.s3.example/thumbnail?X-Amz-Signature=s");
        given(fileStoragePort.createDownloadUrl(any())).willReturn(signed);

        var result = service.list(new ListTreatmentRecordsUseCase.Query(
                USER_ID, ServiceType.CUT, "  김실장 ", " 준헤어  ",
                PERFORMED_AT.minusSeconds(60), PERFORMED_AT.plusSeconds(60),
                1, 2, "performedAt,asc"));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).thumbnailUrl()).isEqualTo(signed);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
        ArgumentCaptor<TreatmentRecordFilter> filter =
                ArgumentCaptor.forClass(TreatmentRecordFilter.class);
        verify(recordRepositoryPort).findPage(filter.capture());
        assertThat(filter.getValue().designerName()).isEqualTo("김실장");
        assertThat(filter.getValue().salonName()).isEqualTo("준헤어");
        assertThat(filter.getValue().ascending()).isTrue();
    }

    @Test
    void returnsAnEmptyPageWithoutTouchingFileStorage() {
        given(recordRepositoryPort.findPage(any()))
                .willReturn(new TreatmentRecordPage(List.of(), 0));

        var result = service.list(new ListTreatmentRecordsUseCase.Query(
                USER_ID, null, null, null, null, null,
                0, 20, "performedAt,desc"));

        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verifyNoInteractions(fileRepositoryPort, fileStoragePort);
    }

    @Test
    void rejectsInvalidListConditionsBeforeTouchingTheRepository() {
        List<ListTreatmentRecordsUseCase.Query> invalidQueries = List.of(
                new ListTreatmentRecordsUseCase.Query(USER_ID, null, null, null,
                        PERFORMED_AT, PERFORMED_AT.minusSeconds(1), 0, 20, "performedAt,desc"),
                new ListTreatmentRecordsUseCase.Query(USER_ID, null, null, null,
                        null, null, -1, 20, "performedAt,desc"),
                new ListTreatmentRecordsUseCase.Query(USER_ID, null, null, null,
                        null, null, 0, 0, "performedAt,desc"),
                new ListTreatmentRecordsUseCase.Query(USER_ID, null, null, null,
                        null, null, 0, 101, "performedAt,desc"),
                new ListTreatmentRecordsUseCase.Query(USER_ID, null, null, null,
                        null, null, 0, 20, "createdAt,desc")
        );

        invalidQueries.forEach(query -> assertThatThrownBy(() -> service.list(query))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST));
        verifyNoInteractions(recordRepositoryPort);
    }

    // ------------------------------------------------------------------ 수정·삭제

    @Test
    void updatesOnlyPresentedFieldsAndClearsExplicitNulls() {
        TreatmentRecord current = TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.CUT), "준헤어", "김실장", PERFORMED_AT,
                3, 100_000L, "KRW", UUID.randomUUID(), "기존 메모", "기존 주의사항");
        given(recordRepositoryPort.findByIdAndUserId(current.recordId(), USER_ID))
                .willReturn(Optional.of(current));
        given(recordRepositoryPort.update(any())).willAnswer(invocation ->
                Optional.of(invocation.getArgument(0)));

        var absentString = UpdateTreatmentRecordUseCase.Patch.<String>absent();
        var command = new UpdateTreatmentRecordUseCase.Command(
                USER_ID, current.recordId(),
                UpdateTreatmentRecordUseCase.Patch.absent(),
                absentString,
                absentString,
                UpdateTreatmentRecordUseCase.Patch.absent(),
                UpdateTreatmentRecordUseCase.Patch.present(5),
                UpdateTreatmentRecordUseCase.Patch.absent(),
                UpdateTreatmentRecordUseCase.Patch.absent(),
                UpdateTreatmentRecordUseCase.Patch.absent(),
                UpdateTreatmentRecordUseCase.Patch.present("  새 메모  "),
                UpdateTreatmentRecordUseCase.Patch.present(null));

        TreatmentRecord updated = service.update(command);

        assertThat(updated.serviceTypes()).isEqualTo(current.serviceTypes());
        assertThat(updated.salonName()).isEqualTo(current.salonName());
        assertThat(updated.satisfaction()).isEqualTo(5);
        assertThat(updated.memo()).isEqualTo("새 메모");
        assertThat(updated.nextVisitCautions()).isNull();
        verify(recordRepositoryPort).update(updated);
    }

    @Test
    void rejectsFuturePerformedAtDuringUpdate() {
        TreatmentRecord current = TreatmentRecord.create(
                USER_ID, Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                null, null, null, null);
        given(recordRepositoryPort.findByIdAndUserId(current.recordId(), USER_ID))
                .willReturn(Optional.of(current));
        var command = updateOnlyPerformedAt(
                current.recordId(), Instant.now().plusSeconds(86_400));

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(TreatmentException.class)
                .hasFieldOrPropertyWithValue("error", TreatmentError.PERFORMED_AT_IN_FUTURE);
        verify(recordRepositoryPort, never()).update(any());
    }

    @Test
    void deletesOwnRecordAfterMarkingEveryAttachedFileDeleted() {
        UUID recordId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        TreatmentPhoto photo = new TreatmentPhoto(
                UUID.randomUUID(), recordId, fileId, ImageType.AFTER, Instant.now());
        TreatmentRecord record = new TreatmentRecord(
                recordId, USER_ID, Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                null, null, null, null, List.of(photo), Instant.now());
        StoredFile file = readyFile(fileId);
        given(recordRepositoryPort.findByIdAndUserId(recordId, USER_ID))
                .willReturn(Optional.of(record));
        given(fileRepositoryPort.findById(fileId)).willReturn(Optional.of(file));
        given(fileRepositoryPort.transition(any(), any())).willAnswer(invocation ->
                invocation.getArgument(0));
        given(recordRepositoryPort.deleteById(recordId)).willReturn(true);

        service.delete(new DeleteTreatmentRecordUseCase.Command(USER_ID, recordId));

        verify(fileRepositoryPort).transition(file.markDeleted(), FileStatus.READY);
        verify(recordRepositoryPort).deleteById(recordId);
    }

    @Test
    void hidesAnotherUsersRecordForUpdateAndDelete() {
        TreatmentRecord foreign = TreatmentRecord.create(
                UUID.randomUUID(), Set.of(ServiceType.CUT), null, null, PERFORMED_AT,
                null, null, null, null);
        given(recordRepositoryPort.findByIdAndUserId(foreign.recordId(), USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(updateOnlyPerformedAt(
                foreign.recordId(), PERFORMED_AT.minusSeconds(1))))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
        assertThatThrownBy(() -> service.delete(
                new DeleteTreatmentRecordUseCase.Command(USER_ID, foreign.recordId())))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
        verifyNoInteractions(fileRepositoryPort, fileStoragePort);
        verify(recordRepositoryPort, never()).update(any());
        verify(recordRepositoryPort, never()).deleteById(any());
    }

    // ------------------------------------------------------------------ 헬퍼

    private Command command(Instant performedAt, List<Command.Photo> photos) {
        return new Command(USER_ID, Set.of(ServiceType.CUT, ServiceType.COLOR), null, null,
                performedAt, null, null, null, null, photos);
    }

    private UpdateTreatmentRecordUseCase.Command updateOnlyPerformedAt(
            UUID recordId,
            Instant performedAt
    ) {
        return new UpdateTreatmentRecordUseCase.Command(
                USER_ID, recordId,
                UpdateTreatmentRecordUseCase.Patch.absent(),
                UpdateTreatmentRecordUseCase.Patch.absent(),
                UpdateTreatmentRecordUseCase.Patch.absent(),
                UpdateTreatmentRecordUseCase.Patch.present(performedAt),
                UpdateTreatmentRecordUseCase.Patch.absent(),
                UpdateTreatmentRecordUseCase.Patch.absent(),
                UpdateTreatmentRecordUseCase.Patch.absent(),
                UpdateTreatmentRecordUseCase.Patch.absent(),
                UpdateTreatmentRecordUseCase.Patch.absent(),
                UpdateTreatmentRecordUseCase.Patch.absent());
    }

    private StoredFile readyFile(UUID fileId) {
        return readyFileOwnedBy(fileId, USER_ID);
    }

    private StoredFile readyFileOwnedBy(UUID fileId, UUID userId) {
        return new StoredFile(fileId, UUID.randomUUID(), userId, FilePurpose.TREATMENT_PHOTO,
                FileStatus.READY, "TREATMENT_PHOTO/k", "image/jpeg", "a.jpg", 10, null,
                null, null, Instant.now().plusSeconds(300), Instant.now());
    }

    private StoredFile fileInStatus(UUID fileId, FileStatus status) {
        return new StoredFile(fileId, UUID.randomUUID(), USER_ID, FilePurpose.TREATMENT_PHOTO,
                status, "TREATMENT_PHOTO/k", "image/jpeg", "a.jpg", 10, null,
                null, null, Instant.now().plusSeconds(300), Instant.now());
    }
}
