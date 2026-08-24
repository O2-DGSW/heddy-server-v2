package com.heddy.application.treatment.service;

import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.treatment.exception.TreatmentError;
import com.heddy.domain.treatment.exception.TreatmentException;
import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.port.in.CreateTreatmentRecordUseCase.Command;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private TreatmentRecordService service;

    @BeforeEach
    void setUp() {
        service = new TreatmentRecordService(recordRepositoryPort, fileRepositoryPort);
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

    // ------------------------------------------------------------------ 헬퍼

    private Command command(Instant performedAt, List<Command.Photo> photos) {
        return new Command(USER_ID, Set.of(ServiceType.CUT, ServiceType.COLOR), null, null,
                performedAt, null, null, null, null, photos);
    }

    private StoredFile readyFile(UUID fileId) {
        return readyFileOwnedBy(fileId, USER_ID);
    }

    private StoredFile readyFileOwnedBy(UUID fileId, UUID userId) {
        return new StoredFile(fileId, UUID.randomUUID(), userId, FilePurpose.TREATMENT_PHOTO,
                FileStatus.READY, "TREATMENT_PHOTO/k", "image/jpeg", "a.jpg", 10, null,
                null, null, Instant.now().plusSeconds(300), Instant.now());
    }
}
