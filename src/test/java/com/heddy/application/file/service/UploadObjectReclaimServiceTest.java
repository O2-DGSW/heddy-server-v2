package com.heddy.application.file.service;

import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UploadObjectReclaimServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock FileRepositoryPort fileRepositoryPort;
    @Mock FileStoragePort fileStoragePort;

    private UploadObjectReclaimService service;

    @BeforeEach
    void setUp() {
        service = new UploadObjectReclaimService(fileRepositoryPort, fileStoragePort);
    }

    /**
     * 취소가 지운 객체는 살아 있는 presigned PUT URL 로 되살아날 수 있어 최종 회수가 아니다.
     * 만료 이후 같은 키를 한 번 더 지우고 그때 회수를 확정한다.
     */
    @Test
    void deletesTheObjectAgainAfterExpiryAndMarksTheSessionReclaimed() {
        StoredFile cancelled = cancelledSession();
        given(fileRepositoryPort.findReclaimTargets(any(), anyInt()))
                .willReturn(List.of(cancelled));

        assertThat(service.reclaimExpired(10)).isEqualTo(1);

        verify(fileStoragePort).deleteObject(cancelled.objectKey());
        verify(fileRepositoryPort).markReclaimed(eq(cancelled.fileId()), any(Instant.class));
    }

    /**
     * 삭제에 실패한 세션은 표시하지 않는다. 표시해버리면 객체가 남은 채 대상에서 빠져 영영
     * 회수되지 않는다. 한 건의 실패가 나머지 대상까지 막지도 않는다.
     */
    @Test
    void leavesFailedSessionsUnmarkedAndKeepsGoing() {
        StoredFile failing = cancelledSession();
        StoredFile succeeding = cancelledSession();
        given(fileRepositoryPort.findReclaimTargets(any(), anyInt()))
                .willReturn(List.of(failing, succeeding));
        doThrow(new RuntimeException("storage down"))
                .when(fileStoragePort).deleteObject(failing.objectKey());

        assertThat(service.reclaimExpired(10)).isEqualTo(1);

        verify(fileRepositoryPort, never()).markReclaimed(eq(failing.fileId()), any());
        verify(fileRepositoryPort).markReclaimed(eq(succeeding.fileId()), any());
    }

    @Test
    void doesNothingWhenNoSessionAwaitsReclaim() {
        given(fileRepositoryPort.findReclaimTargets(any(), anyInt())).willReturn(List.of());

        assertThat(service.reclaimExpired(10)).isZero();

        verify(fileStoragePort, never()).deleteObject(any());
    }

    private static StoredFile cancelledSession() {
        return StoredFile.pending(
                USER_ID, FilePurpose.TREATMENT_PHOTO,
                "TREATMENT_PHOTO/" + USER_ID + "/" + UUID.randomUUID(),
                "image/jpeg", "after.jpg", 1_024, null,
                Instant.now().minus(1, ChronoUnit.MINUTES))
                .markDeleted();
    }
}
