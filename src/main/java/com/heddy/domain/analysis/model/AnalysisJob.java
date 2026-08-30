package com.heddy.domain.analysis.model;

import com.heddy.domain.analysis.exception.AnalysisError;
import com.heddy.domain.analysis.exception.AnalysisException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 사진 한 장에 대한 분석 작업 한 건. 분석 도메인의 애그리게이트 루트다.
 *
 * <p>불변식은 이 모델만 책임진다.
 * <ul>
 *   <li>상태 전이는 {@link AnalysisJobStatus} 가 허용한 것만</li>
 *   <li>진행률은 0~100</li>
 *   <li>실패·재촬영 안내에는 사유 코드가 있어야 한다 — 코드 없는 실패는 앱이 안내할 말이 없다</li>
 *   <li>시도 횟수는 1 이상 — 접수된 순간이 이미 첫 시도다</li>
 * </ul>
 *
 * <p>소유자를 기록에서 조인하지 않고 직접 들고 있다. 남의 작업은 없는 작업과 같은 404 여야 하고
 * 질의 횟수도 같아야 하는데(#31 컨벤션), 그러려면 소유자 조건이 작업 조회 한 번에 함께 실려야
 * 한다.
 */
public record AnalysisJob(
        UUID jobId,
        UUID userId,
        UUID recordId,
        UUID photoId,
        AnalysisJobStatus status,
        int progress,
        int attemptCount,
        String failureCode,
        String failureMessage,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt
) {
    private static final int COMPLETE = 100;

    public AnalysisJob {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(status, "status");

        // photoId 는 비어 있을 수 있다. 분석 대상 사진이 지워져도 분석 이력은 남기 때문이다.
        // 사진 삭제는 분석을 무효화하는 사건이지 이력을 지우는 사건이 아니다.
        if (photoId == null && status.isInProgress()) {
            throw new AnalysisException(AnalysisError.JOB_PHOTO_REQUIRED);
        }

        if (progress < 0 || progress > COMPLETE) {
            throw new AnalysisException(AnalysisError.JOB_PROGRESS_INVALID);
        }
        if (attemptCount < 1) {
            throw new AnalysisException(AnalysisError.JOB_ATTEMPT_COUNT_INVALID);
        }
        if (status.requiresFailureReason() && (failureCode == null || failureCode.isBlank())) {
            throw new AnalysisException(AnalysisError.JOB_FAILURE_REASON_REQUIRED);
        }
    }

    /** 새 작업을 접수한다. 접수된 순간이 첫 시도라 시도 횟수는 1 에서 시작한다. */
    public static AnalysisJob create(UUID userId, UUID recordId, UUID photoId, Instant now) {
        Objects.requireNonNull(photoId, "photoId");
        return new AnalysisJob(UUID.randomUUID(), userId, recordId, photoId,
                AnalysisJobStatus.PENDING, 0, 1, null, null, null, null, now);
    }

    /** 이미 읽어 온 행을 도메인으로 되돌릴 때 쓴다. 불변식을 다시 통과한다. */
    public static AnalysisJob reconstitute(
            UUID jobId, UUID userId, UUID recordId, UUID photoId, AnalysisJobStatus status,
            int progress, int attemptCount, String failureCode, String failureMessage,
            Instant startedAt, Instant finishedAt, Instant createdAt
    ) {
        return new AnalysisJob(jobId, userId, recordId, photoId, status, progress, attemptCount,
                failureCode, failureMessage, startedAt, finishedAt, createdAt);
    }

    public AnalysisJob start(Instant now) {
        return moveTo(AnalysisJobStatus.PROCESSING, progress, null, null, now, null);
    }

    /** 진행률만 갱신한다. 끝난 작업의 진행률은 더 이상 움직이지 않는다. */
    public AnalysisJob progressTo(int percent) {
        if (status != AnalysisJobStatus.PROCESSING) {
            throw new AnalysisException(AnalysisError.JOB_TRANSITION_INVALID);
        }
        return new AnalysisJob(jobId, userId, recordId, photoId, status, percent, attemptCount,
                failureCode, failureMessage, startedAt, finishedAt, createdAt);
    }

    public AnalysisJob succeed(Instant now) {
        return moveTo(AnalysisJobStatus.SUCCEEDED, COMPLETE, null, null, startedAt, now);
    }

    public AnalysisJob fail(String code, String message, Instant now) {
        return moveTo(AnalysisJobStatus.FAILED, progress, code, message, startedAt, now);
    }

    /** 머리 미검출·낮은 신뢰도·촬영 부적합. 실패가 아니라 재촬영 안내라 재시도 대상이 아니다. */
    public AnalysisJob markUnavailable(String code, String message, Instant now) {
        return moveTo(AnalysisJobStatus.UNAVAILABLE, progress, code, message, startedAt, now);
    }

    /**
     * 사진이 바뀌어 결과가 현재 사진을 반영하지 않게 됐다. 이미 끝난 작업이라면 완료 시각은
     * 그대로 둔다 — 무효화된 시점이 분석이 끝난 시점을 덮어쓰면 언제 분석했는지가 사라진다.
     */
    public AnalysisJob markStale(Instant now) {
        return moveTo(AnalysisJobStatus.STALE, progress, failureCode, failureMessage,
                startedAt, finishedAt == null ? now : finishedAt);
    }

    /**
     * 재시도한다. 기존 행을 되돌리지 않고 시도 횟수를 이어받은 새 작업을 만든다 — 실패한 시도가
     * 이력으로 남아야 몇 번째에 무엇이 실패했는지 추적할 수 있다.
     *
     * <p>{@code UNAVAILABLE} 은 재시도 대상이 아니다. 같은 사진을 다시 분석해도 결과가 같아
     * 앱이 재촬영을 안내해야 한다.
     */
    public AnalysisJob retry(Instant now) {
        if (status != AnalysisJobStatus.FAILED) {
            throw new AnalysisException(AnalysisError.JOB_RETRY_NOT_ALLOWED);
        }
        return new AnalysisJob(UUID.randomUUID(), userId, recordId, photoId,
                AnalysisJobStatus.PENDING, 0, attemptCount + 1, null, null, null, null, now);
    }

    private AnalysisJob moveTo(
            AnalysisJobStatus next, int nextProgress, String code, String message,
            Instant nextStartedAt, Instant nextFinishedAt
    ) {
        if (!status.canMoveTo(next)) {
            throw new AnalysisException(AnalysisError.JOB_TRANSITION_INVALID);
        }
        return new AnalysisJob(jobId, userId, recordId, photoId, next, nextProgress, attemptCount,
                code, message, nextStartedAt, nextFinishedAt, createdAt);
    }
}
