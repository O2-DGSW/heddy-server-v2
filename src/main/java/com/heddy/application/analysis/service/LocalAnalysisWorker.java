package com.heddy.application.analysis.service;

import com.heddy.domain.analysis.model.HairAnalysisOutcome;
import com.heddy.domain.analysis.port.out.HairAnalysisEnginePort;
import com.heddy.domain.file.port.out.FileStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 작업 커밋 이후 S3 원본을 읽고 로컬 ONNX 모델을 실행한다. */
@Component
public class LocalAnalysisWorker {

    private static final Logger log = LoggerFactory.getLogger(LocalAnalysisWorker.class);

    private final AnalysisLifecycleService lifecycleService;
    private final FileStoragePort fileStoragePort;
    private final HairAnalysisEnginePort enginePort;
    private final long maximumImageBytes;

    public LocalAnalysisWorker(
            AnalysisLifecycleService lifecycleService,
            FileStoragePort fileStoragePort,
            HairAnalysisEnginePort enginePort,
            @Value("${app.ai.maximum-image-bytes}") long maximumImageBytes
    ) {
        this.lifecycleService = lifecycleService;
        this.fileStoragePort = fileStoragePort;
        this.enginePort = enginePort;
        this.maximumImageBytes = maximumImageBytes;
    }

    @Async("hairAnalysisTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AnalysisJobAcceptedEvent event) {
        try {
            AnalysisLifecycleService.WorkItem work = lifecycleService.start(
                    event.jobId(), event.userId());
            byte[] image = fileStoragePort.readObject(work.file(), maximumImageBytes);
            HairAnalysisOutcome outcome = enginePort.analyze(image);
            lifecycleService.finish(event.jobId(), event.userId(), outcome);
        } catch (Exception failure) {
            log.error("로컬 헤어 분석 실패. jobId={}", event.jobId(), failure);
            lifecycleService.fail(event.jobId(), event.userId(), failure);
        }
    }
}
