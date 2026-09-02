package com.heddy.application.analysis.service;

import com.heddy.domain.analysis.model.AnalysisJob;
import com.heddy.domain.analysis.model.AnalysisJobStatus;
import com.heddy.domain.analysis.model.AnalysisOverlay;
import com.heddy.domain.analysis.model.AnalysisResult;
import com.heddy.domain.analysis.model.ConfidenceGrade;
import com.heddy.domain.analysis.model.MetricScore;
import com.heddy.domain.analysis.model.MetricType;
import com.heddy.domain.analysis.model.OverlayType;
import com.heddy.domain.analysis.port.in.GetLatestAnalysisUseCase;
import com.heddy.domain.analysis.port.out.AnalysisJobRepositoryPort;
import com.heddy.domain.analysis.port.out.AnalysisOverlayRepositoryPort;
import com.heddy.domain.analysis.port.out.AnalysisResultRepositoryPort;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetLatestAnalysisServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID PHOTO_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    @Mock TreatmentRecordRepositoryPort recordRepositoryPort;
    @Mock AnalysisResultRepositoryPort resultRepositoryPort;
    @Mock AnalysisOverlayRepositoryPort overlayRepositoryPort;
    @Mock AnalysisJobRepositoryPort jobRepositoryPort;

    @InjectMocks GetLatestAnalysisService service;

    @Test
    void returnsTheLatestAnalysisWithItsJobStatusAndOverlays() {
        AnalysisJob job = succeededJob();
        AnalysisResult analysis = resultOf(job);
        givenOwnedRecord();
        given(resultRepositoryPort.findLatestByRecordId(RECORD_ID))
                .willReturn(Optional.of(analysis));
        given(jobRepositoryPort.findByIdAndUserId(job.jobId(), USER_ID))
                .willReturn(Optional.of(job));
        given(overlayRepositoryPort.findByAnalysisId(analysis.analysisId()))
                .willReturn(List.of(AnalysisOverlay.create(
                        analysis.analysisId(), OverlayType.HAIR_MASK, UUID.randomUUID(), NOW)));

        var result = service.get(query(USER_ID));

        assertThat(result.analysis().analysisId()).isEqualTo(analysis.analysisId());
        assertThat(result.status()).isEqualTo(AnalysisJobStatus.SUCCEEDED);
        assertThat(result.overlays()).hasSize(1);
    }

    /** 사진이 바뀐 뒤에도 결과는 그대로 내려가고, 옛 사진의 것이라는 사실은 상태로 알린다. */
    @Test
    void reportsStaleWithoutHidingTheResult() {
        AnalysisJob stale = succeededJob().markStale(NOW);
        AnalysisResult analysis = resultOf(stale);
        givenOwnedRecord();
        given(resultRepositoryPort.findLatestByRecordId(RECORD_ID))
                .willReturn(Optional.of(analysis));
        given(jobRepositoryPort.findByIdAndUserId(stale.jobId(), USER_ID))
                .willReturn(Optional.of(stale));
        given(overlayRepositoryPort.findByAnalysisId(analysis.analysisId()))
                .willReturn(List.of());

        var result = service.get(query(USER_ID));

        assertThat(result.status()).isEqualTo(AnalysisJobStatus.STALE);
        assertThat(result.analysis()).isNotNull();
    }

    /** 남의 기록은 분석이 있는지조차 드러내면 안 된다 — 결과 조회 자체를 하지 않는다. */
    @Test
    void hidesAnalysesOfSomeoneElsesRecord() {
        given(recordRepositoryPort.findByIdAndUserId(RECORD_ID, OTHER_USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(query(OTHER_USER_ID)))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
        verifyNoInteractions(resultRepositoryPort, overlayRepositoryPort, jobRepositoryPort);
    }

    @Test
    void answersNotFoundWhenTheRecordWasNeverAnalysed() {
        givenOwnedRecord();
        given(resultRepositoryPort.findLatestByRecordId(RECORD_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(query(USER_ID)))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ------------------------------------------------------------------ 헬퍼

    private void givenOwnedRecord() {
        given(recordRepositoryPort.findByIdAndUserId(RECORD_ID, USER_ID))
                .willReturn(Optional.of(new TreatmentRecord(RECORD_ID, USER_ID,
                        Set.of(ServiceType.CUT), null, null, NOW.minusSeconds(3600), null,
                        null, null, null, List.of(), NOW)));
    }

    private GetLatestAnalysisUseCase.Query query(UUID requesterId) {
        return new GetLatestAnalysisUseCase.Query(requesterId, RECORD_ID);
    }

    private AnalysisJob succeededJob() {
        return AnalysisJob.create(USER_ID, RECORD_ID, PHOTO_ID, NOW).start(NOW).succeed(NOW);
    }

    private AnalysisResult resultOf(AnalysisJob job) {
        Map<MetricType, MetricScore> metrics = new EnumMap<>(MetricType.class);
        for (MetricType type : MetricType.values()) {
            metrics.put(type, MetricScore.of("78.00", ConfidenceGrade.HIGH));
        }
        return AnalysisResult.create(job, metrics,
                MetricScore.of("82.00", ConfidenceGrade.HIGH), "hair-v1.2.0", null, null, NOW);
    }
}
