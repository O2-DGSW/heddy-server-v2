package com.heddy.application.analysis.service;

import com.heddy.domain.analysis.model.AnalysisJob;
import com.heddy.domain.analysis.model.AnalysisJobStatus;
import com.heddy.domain.analysis.model.AnalysisResult;
import com.heddy.domain.analysis.model.HairAnalysisOutcome;
import com.heddy.domain.analysis.port.out.AnalysisJobRepositoryPort;
import com.heddy.domain.analysis.port.out.AnalysisResultRepositoryPort;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.treatment.model.TreatmentPhoto;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** 느린 모델 추론 바깥의 짧은 DB 트랜잭션들을 담당한다. */
@Service
public class AnalysisLifecycleService {

    private final AnalysisJobRepositoryPort jobRepositoryPort;
    private final AnalysisResultRepositoryPort resultRepositoryPort;
    private final TreatmentRecordRepositoryPort recordRepositoryPort;
    private final FileRepositoryPort fileRepositoryPort;

    public AnalysisLifecycleService(
            AnalysisJobRepositoryPort jobRepositoryPort,
            AnalysisResultRepositoryPort resultRepositoryPort,
            TreatmentRecordRepositoryPort recordRepositoryPort,
            FileRepositoryPort fileRepositoryPort
    ) {
        this.jobRepositoryPort = jobRepositoryPort;
        this.resultRepositoryPort = resultRepositoryPort;
        this.recordRepositoryPort = recordRepositoryPort;
        this.fileRepositoryPort = fileRepositoryPort;
    }

    @Transactional
    public WorkItem start(UUID jobId, UUID userId) {
        AnalysisJob job = jobRepositoryPort.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new IllegalStateException("분석 작업이 없습니다: " + jobId));
        if (job.photoId() == null) {
            throw new IllegalStateException("분석 작업의 사진이 없습니다");
        }
        TreatmentRecord record = recordRepositoryPort.findByIdAndUserId(job.recordId(), userId)
                .orElseThrow(() -> new IllegalStateException("분석 대상 기록이 없습니다"));
        TreatmentPhoto photo = record.photos().stream()
                .filter(candidate -> candidate.photoId().equals(job.photoId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("분석 대상 사진이 없습니다"));
        StoredFile file = fileRepositoryPort.findById(photo.fileId())
                .filter(StoredFile::isReady)
                .orElseThrow(() -> new IllegalStateException("분석 대상 파일이 READY 상태가 아닙니다"));
        AnalysisJob processing = jobRepositoryPort.update(job.start(Instant.now()).progressTo(10));
        return new WorkItem(processing, file);
    }

    @Transactional
    public void finish(UUID jobId, UUID userId, HairAnalysisOutcome outcome) {
        AnalysisJob current = jobRepositoryPort.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new IllegalStateException("분석 작업이 없습니다: " + jobId));
        // 분석 도중 사진이 교체되면 staleness 서비스가 STALE 로 만든다. 옛 사진 결과를 저장하지 않는다.
        if (current.status() == AnalysisJobStatus.STALE) {
            return;
        }
        Instant now = Instant.now();
        if (outcome instanceof HairAnalysisOutcome.Unavailable unavailable) {
            jobRepositoryPort.update(current.markUnavailable(
                    unavailable.code(), unavailable.message(), now));
            return;
        }
        HairAnalysisOutcome.Succeeded succeeded = (HairAnalysisOutcome.Succeeded) outcome;
        // 워커가 같은 사건을 중복 전달받아도 작업 하나에 결과를 두 번 만들지 않는다.
        if (resultRepositoryPort.findByJobId(current.jobId()).isEmpty()) {
            var prediction = succeeded.prediction();
            resultRepositoryPort.insert(AnalysisResult.create(
                    current, prediction.metrics(), prediction.confidence(),
                    prediction.modelVersion(), prediction.summary(), prediction.evidence(), now));
        }
        jobRepositoryPort.update(current.succeed(now));
    }

    @Transactional
    public void fail(UUID jobId, UUID userId, Throwable failure) {
        jobRepositoryPort.findByIdAndUserId(jobId, userId)
                .filter(job -> job.status() == AnalysisJobStatus.PENDING
                        || job.status() == AnalysisJobStatus.PROCESSING)
                .map(job -> job.fail("ANALYSIS_EXECUTION_FAILED",
                        safeMessage(failure), Instant.now()))
                .ifPresent(jobRepositoryPort::update);
    }

    private static String safeMessage(Throwable failure) {
        // 실제 예외는 워커 로그에만 남긴다. 파일 경로·S3 키·모델 경로가 API로 노출되면 안 된다.
        return "헤어 분석 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
    }

    public record WorkItem(AnalysisJob job, StoredFile file) {
    }
}
