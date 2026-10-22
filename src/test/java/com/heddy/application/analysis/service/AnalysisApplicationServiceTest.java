package com.heddy.application.analysis.service;

import com.heddy.domain.analysis.model.AnalysisJob;
import com.heddy.domain.analysis.port.in.RequestAnalysisUseCase;
import com.heddy.domain.analysis.port.out.*;
import com.heddy.domain.file.model.*;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.treatment.model.*;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AnalysisApplicationServiceTest {
    @Test void reattachesToOwnedPendingJobWithoutDuplicateInferenceAfterReload() {
        UUID user = UUID.randomUUID(), recordId = UUID.randomUUID(), photoId = UUID.randomUUID();
        var records = mock(TreatmentRecordRepositoryPort.class);
        var files = mock(FileRepositoryPort.class);
        var jobs = mock(AnalysisJobRepositoryPort.class);
        var engine = mock(HairAnalysisEnginePort.class);
        var events = mock(ApplicationEventPublisher.class);
        var file = new StoredFile(UUID.randomUUID(), UUID.randomUUID(), user, FilePurpose.TREATMENT_PHOTO,
                FileStatus.READY, "object", "image/jpeg", "photo.jpg", 100, null, null, null,
                Instant.now(), Instant.now(), null);
        var photo = new TreatmentPhoto(photoId, recordId, file.fileId(), ImageType.AFTER, 0, Instant.now());
        var record = new TreatmentRecord(recordId, user, Set.of(ServiceType.CUT), null, null,
                Instant.now(), null, null, null, null, null, null, List.of(photo), Instant.now());
        var existing = AnalysisJob.create(user, recordId, photoId, Instant.now());
        when(engine.isReady()).thenReturn(true);
        when(records.findByIdAndUserId(recordId, user)).thenReturn(Optional.of(record));
        when(files.findById(file.fileId())).thenReturn(Optional.of(file));
        when(jobs.findInProgressByPhotoId(photoId)).thenReturn(Optional.of(existing));
        var service = new AnalysisApplicationService(records, files, jobs,
                mock(AnalysisResultRepositoryPort.class), mock(AnalysisOverlayRepositoryPort.class), engine, events, 3);
        assertThat(service.request(new RequestAnalysisUseCase.Command(user, recordId, photoId))).isEqualTo(existing);
        verify(jobs, never()).insert(any());
        verifyNoInteractions(events);
    }
}
