package com.heddy.application.analysis.service;

import com.heddy.domain.analysis.model.AnalysisJob;
import com.heddy.domain.analysis.model.AnalysisJobStatus;
import com.heddy.domain.analysis.model.AnalysisResult;
import com.heddy.domain.analysis.port.in.GetAnalysisJobUseCase;
import com.heddy.domain.analysis.port.in.GetAnalysisUseCase;
import com.heddy.domain.analysis.port.in.RequestAnalysisUseCase;
import com.heddy.domain.analysis.port.in.RetryAnalysisUseCase;
import com.heddy.domain.analysis.port.out.AnalysisJobRepositoryPort;
import com.heddy.domain.analysis.port.out.AnalysisOverlayRepositoryPort;
import com.heddy.domain.analysis.port.out.AnalysisResultRepositoryPort;
import com.heddy.domain.analysis.port.out.HairAnalysisEnginePort;
import com.heddy.domain.file.model.FilePurpose;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.model.TreatmentPhoto;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AnalysisApplicationService implements RequestAnalysisUseCase, GetAnalysisJobUseCase,
        RetryAnalysisUseCase, GetAnalysisUseCase {

    private final TreatmentRecordRepositoryPort recordRepositoryPort;
    private final FileRepositoryPort fileRepositoryPort;
    private final AnalysisJobRepositoryPort jobRepositoryPort;
    private final AnalysisResultRepositoryPort resultRepositoryPort;
    private final AnalysisOverlayRepositoryPort overlayRepositoryPort;
    private final HairAnalysisEnginePort enginePort;
    private final ApplicationEventPublisher eventPublisher;
    private final int maximumAttempts;

    public AnalysisApplicationService(
            TreatmentRecordRepositoryPort recordRepositoryPort,
            FileRepositoryPort fileRepositoryPort,
            AnalysisJobRepositoryPort jobRepositoryPort,
            AnalysisResultRepositoryPort resultRepositoryPort,
            AnalysisOverlayRepositoryPort overlayRepositoryPort,
            HairAnalysisEnginePort enginePort,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.ai.max-attempts}") int maximumAttempts
    ) {
        this.recordRepositoryPort = recordRepositoryPort;
        this.fileRepositoryPort = fileRepositoryPort;
        this.jobRepositoryPort = jobRepositoryPort;
        this.resultRepositoryPort = resultRepositoryPort;
        this.overlayRepositoryPort = overlayRepositoryPort;
        this.enginePort = enginePort;
        this.eventPublisher = eventPublisher;
        this.maximumAttempts = maximumAttempts;
    }

    @Override
    @Transactional
    public AnalysisJob request(RequestAnalysisUseCase.Command command) {
        requireEngine();
        TreatmentRecord record = ownedRecord(command.requesterId(), command.recordId());
        TreatmentPhoto photo = selectPhoto(record, command.photoId());
        requireReadyInput(command.requesterId(), photo);
        if (jobRepositoryPort.findInProgressByPhotoId(photo.photoId()).isPresent()) {
            throw new ApplicationException(ErrorCode.ANALYSIS_ALREADY_IN_PROGRESS);
        }
        try {
            AnalysisJob saved = jobRepositoryPort.insert(AnalysisJob.create(
                    command.requesterId(), record.recordId(), photo.photoId(), Instant.now()));
            eventPublisher.publishEvent(new AnalysisJobAcceptedEvent(
                    saved.jobId(), saved.userId()));
            return saved;
        } catch (DataIntegrityViolationException concurrentRequest) {
            throw new ApplicationException(ErrorCode.ANALYSIS_ALREADY_IN_PROGRESS);
        }
    }

    @Override
    public GetAnalysisJobUseCase.Result get(GetAnalysisJobUseCase.Query query) {
        AnalysisJob job = jobRepositoryPort.findByIdAndUserId(query.jobId(), query.requesterId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        UUID analysisId = resultRepositoryPort.findByJobId(job.jobId())
                .map(AnalysisResult::analysisId)
                .orElse(null);
        return new GetAnalysisJobUseCase.Result(job, analysisId);
    }

    @Override
    @Transactional
    public AnalysisJob retry(RetryAnalysisUseCase.Command command) {
        requireEngine();
        AnalysisJob failed = jobRepositoryPort.findByIdAndUserId(
                        command.jobId(), command.requesterId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        if (failed.status() != AnalysisJobStatus.FAILED) {
            throw new ApplicationException(ErrorCode.ANALYSIS_RETRY_NOT_ALLOWED);
        }
        if (failed.attemptCount() >= maximumAttempts) {
            throw new ApplicationException(ErrorCode.ANALYSIS_RETRY_LIMIT_EXCEEDED);
        }
        if (failed.photoId() == null) {
            throw new ApplicationException(ErrorCode.ANALYSIS_PHOTO_REQUIRED);
        }
        TreatmentRecord record = ownedRecord(command.requesterId(), failed.recordId());
        TreatmentPhoto photo = selectPhoto(record, failed.photoId());
        requireReadyInput(command.requesterId(), photo);
        AnalysisJob retried = jobRepositoryPort.insert(failed.retry(Instant.now()));
        eventPublisher.publishEvent(new AnalysisJobAcceptedEvent(
                retried.jobId(), retried.userId()));
        return retried;
    }

    @Override
    public GetAnalysisUseCase.Result get(GetAnalysisUseCase.Query query) {
        AnalysisResult result = resultRepositoryPort.findByIdAndUserId(
                        query.analysisId(), query.requesterId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        AnalysisJobStatus status = jobRepositoryPort.findByIdAndUserId(
                        result.jobId(), query.requesterId())
                .map(AnalysisJob::status)
                .orElseThrow(() -> new IllegalStateException(
                        "분석 결과에 연결된 작업이 없습니다: " + result.analysisId()));
        return new GetAnalysisUseCase.Result(result, status,
                overlayRepositoryPort.findByAnalysisId(result.analysisId()));
    }

    private void requireEngine() {
        if (!enginePort.isReady()) {
            throw new ApplicationException(ErrorCode.ANALYSIS_ENGINE_UNAVAILABLE);
        }
    }

    private TreatmentRecord ownedRecord(UUID userId, UUID recordId) {
        return recordRepositoryPort.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private TreatmentPhoto selectPhoto(TreatmentRecord record, UUID requestedPhotoId) {
        if (requestedPhotoId != null) {
            return record.photos().stream()
                    .filter(photo -> photo.photoId().equals(requestedPhotoId))
                    .findFirst()
                    .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        return record.photos().stream()
                .filter(photo -> photo.imageType() == ImageType.AFTER)
                .min(Comparator.comparingInt(TreatmentPhoto::sortOrder))
                .orElseThrow(() -> new ApplicationException(ErrorCode.ANALYSIS_PHOTO_REQUIRED));
    }

    private StoredFile requireReadyInput(UUID userId, TreatmentPhoto photo) {
        StoredFile file = fileRepositoryPort.findById(photo.fileId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!userId.equals(file.userId())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN_RESOURCE);
        }
        if (!file.isReady() || file.purpose() != FilePurpose.TREATMENT_PHOTO) {
            throw new ApplicationException(ErrorCode.ANALYSIS_INPUT_NOT_READY);
        }
        return file;
    }
}
