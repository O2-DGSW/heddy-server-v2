package com.heddy.adapter.out.persistence.analysis;

import com.heddy.adapter.out.persistence.BaseEntity;
import com.heddy.domain.analysis.exception.AnalysisError;
import com.heddy.domain.analysis.exception.AnalysisException;
import com.heddy.domain.analysis.model.AnalysisJob;
import com.heddy.domain.analysis.model.AnalysisJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 분석 작업의 JPA 표현. 상태를 열거형으로 바로 묶지 않고 이름으로 저장하는 이유는 공유·시술기록과
 * 같다 — 행에서 알 수 없는 이름이 읽힐 때 직렬화 계층의 예외 대신 도메인 오류로 막는다.
 */
@Entity
@Table(name = "analysis_jobs")
class AnalysisJobEntity extends BaseEntity {

    @Id
    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;

    /** 사진이 지워지면 DB 가 NULL 로 비운다(SET NULL). 분석 이력은 그대로 남는다. */
    @Column(name = "photo_id", updatable = false)
    private UUID photoId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "progress", nullable = false)
    private short progress;

    @Column(name = "attempt_count", nullable = false, updatable = false)
    private short attemptCount;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected AnalysisJobEntity() {
    }

    AnalysisJobEntity(AnalysisJob job) {
        jobId = job.jobId();
        userId = job.userId();
        recordId = job.recordId();
        photoId = job.photoId();
        attemptCount = (short) job.attemptCount();
        apply(job);
    }

    /** 상태 전이 결과를 옮겨 담는다. 식별자와 대상, 시도 횟수는 작업이 사는 동안 바뀌지 않는다. */
    void apply(AnalysisJob job) {
        status = job.status().name();
        progress = (short) job.progress();
        failureCode = job.failureCode();
        failureMessage = job.failureMessage();
        startedAt = job.startedAt();
        finishedAt = job.finishedAt();
    }

    AnalysisJob toDomain() {
        return AnalysisJob.reconstitute(jobId, userId, recordId, photoId, parseStatus(),
                progress, attemptCount, failureCode, failureMessage,
                startedAt, finishedAt, getCreatedAt());
    }

    private AnalysisJobStatus parseStatus() {
        try {
            return AnalysisJobStatus.valueOf(status);
        } catch (IllegalArgumentException invalidName) {
            throw new AnalysisException(AnalysisError.JOB_STATUS_UNKNOWN);
        }
    }
}
