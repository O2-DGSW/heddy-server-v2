package com.heddy.application.analysis.service;

import com.heddy.domain.analysis.model.AnalysisJob;
import com.heddy.domain.analysis.model.AnalysisJobStatus;
import com.heddy.domain.analysis.port.out.AnalysisJobRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalysisStalenessServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID PHOTO_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-30T10:00:00Z");

    @Mock AnalysisJobRepositoryPort jobRepositoryPort;

    @InjectMocks AnalysisStalenessService service;

    @Test
    void stalesTheLatestCompletedAnalysis() {
        given(jobRepositoryPort.findLatestByRecordId(RECORD_ID))
                .willReturn(Optional.of(pending().start(NOW).succeed(NOW)));

        service.markLatestStale(RECORD_ID);

        ArgumentCaptor<AnalysisJob> saved = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(jobRepositoryPort).update(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(AnalysisJobStatus.STALE);
    }

    /** 지금 도착할 결과는 이미 옛 사진의 것이고, 진행 중 자리를 비워야 다시 분석을 걸 수 있다. */
    @Test
    void stalesAnAnalysisThatIsStillRunning() {
        given(jobRepositoryPort.findLatestByRecordId(RECORD_ID))
                .willReturn(Optional.of(pending().start(NOW)));

        service.markLatestStale(RECORD_ID);

        ArgumentCaptor<AnalysisJob> saved = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(jobRepositoryPort).update(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(AnalysisJobStatus.STALE);
    }

    /** 사진을 연달아 바꿔도, 같은 변경이 두 번 전달돼도 결과가 같아야 한다. */
    @Test
    void doesNothingWhenTheLatestAnalysisIsAlreadyStale() {
        given(jobRepositoryPort.findLatestByRecordId(RECORD_ID))
                .willReturn(Optional.of(pending().start(NOW).succeed(NOW).markStale(NOW)));

        service.markLatestStale(RECORD_ID);

        verify(jobRepositoryPort, never()).update(any());
    }

    /** 실패로 끝난 작업엔 무효화할 결과가 없다. */
    @Test
    void leavesFailedAndUnavailableAnalysesAlone() {
        given(jobRepositoryPort.findLatestByRecordId(RECORD_ID))
                .willReturn(Optional.of(pending().start(NOW).fail("AI_TIMEOUT", "시간 초과", NOW)));

        service.markLatestStale(RECORD_ID);

        verify(jobRepositoryPort, never()).update(any());
    }

    @Test
    void doesNothingForARecordThatWasNeverAnalysed() {
        given(jobRepositoryPort.findLatestByRecordId(RECORD_ID)).willReturn(Optional.empty());

        service.markLatestStale(RECORD_ID);

        verify(jobRepositoryPort, never()).update(any());
    }

    private AnalysisJob pending() {
        return AnalysisJob.create(USER_ID, RECORD_ID, PHOTO_ID, NOW);
    }
}
