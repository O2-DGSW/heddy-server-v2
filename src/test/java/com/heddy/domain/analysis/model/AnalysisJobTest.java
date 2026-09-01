package com.heddy.domain.analysis.model;

import com.heddy.domain.analysis.exception.AnalysisError;
import com.heddy.domain.analysis.exception.AnalysisException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisJobTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID PHOTO_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-30T10:00:00Z");

    @Test
    void startsPendingOnItsFirstAttempt() {
        AnalysisJob job = AnalysisJob.create(USER_ID, RECORD_ID, PHOTO_ID, NOW);

        assertThat(job.status()).isEqualTo(AnalysisJobStatus.PENDING);
        assertThat(job.attemptCount()).isEqualTo(1);
        assertThat(job.progress()).isZero();
        assertThat(job.startedAt()).isNull();
        assertThat(job.finishedAt()).isNull();
    }

    @Test
    void walksThroughProcessingToSuccess() {
        AnalysisJob succeeded = pending().start(NOW).progressTo(40).succeed(NOW.plusSeconds(30));

        assertThat(succeeded.status()).isEqualTo(AnalysisJobStatus.SUCCEEDED);
        assertThat(succeeded.startedAt()).isEqualTo(NOW);
        assertThat(succeeded.finishedAt()).isEqualTo(NOW.plusSeconds(30));
        // 완료된 작업의 진행률이 40 에서 멈춰 있으면 앱이 끝난 작업을 진행 중으로 그린다.
        assertThat(succeeded.progress()).isEqualTo(100);
    }

    /** 전달 자체가 실패할 수 있어 접수 직후에도 실패로 끝난다. */
    @Test
    void allowsFailureBeforeProcessingEverStarts() {
        AnalysisJob failed = pending().fail("AI_DISPATCH_FAILED", "전달 실패", NOW);

        assertThat(failed.status()).isEqualTo(AnalysisJobStatus.FAILED);
    }

    @Test
    void rejectsTransitionsTheStateMachineDoesNotAllow() {
        AnalysisJob succeeded = pending().start(NOW).succeed(NOW);

        assertThatThrownBy(() -> succeeded.start(NOW))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.JOB_TRANSITION_INVALID);
        assertThatThrownBy(() -> succeeded.fail("X", "이미 끝난 작업", NOW))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.JOB_TRANSITION_INVALID);
    }

    /** 실패한 작업엔 무효화할 결과가 없다. */
    @Test
    void refusesToStaleAFailedJob() {
        AnalysisJob failed = pending().start(NOW).fail("AI_TIMEOUT", "시간 초과", NOW);

        assertThatThrownBy(() -> failed.markStale(NOW))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.JOB_TRANSITION_INVALID);
    }

    /** 사진이 바뀌면 아직 끝나지 않은 작업도 무효화된다 — 도착할 결과가 옛 사진의 것이다. */
    @Test
    void stalesJobsThatHaveNotFinishedYet() {
        assertThat(pending().markStale(NOW).status()).isEqualTo(AnalysisJobStatus.STALE);
        assertThat(pending().start(NOW).markStale(NOW).status())
                .isEqualTo(AnalysisJobStatus.STALE);
    }

    /** 무효화 시점이 분석이 끝난 시점을 덮어쓰면 언제 분석했는지가 사라진다. */
    @Test
    void keepsTheOriginalFinishTimeWhenStalingACompletedJob() {
        AnalysisJob succeeded = pending().start(NOW).succeed(NOW.plusSeconds(30));

        assertThat(succeeded.markStale(NOW.plusSeconds(600)).finishedAt())
                .isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void retriesAFailedJobAsANewAttemptThatKeepsTheHistory() {
        AnalysisJob failed = pending().start(NOW).fail("AI_TIMEOUT", "시간 초과", NOW);

        AnalysisJob retried = failed.retry(NOW.plusSeconds(60));

        assertThat(retried.jobId()).isNotEqualTo(failed.jobId());
        assertThat(retried.status()).isEqualTo(AnalysisJobStatus.PENDING);
        assertThat(retried.attemptCount()).isEqualTo(2);
        assertThat(retried.failureCode()).isNull();
        assertThat(retried.photoId()).isEqualTo(failed.photoId());
    }

    /** 재촬영 안내는 같은 사진을 다시 분석해도 결과가 같다. */
    @Test
    void refusesToRetryAnythingButAFailure() {
        AnalysisJob unavailable = pending().start(NOW)
                .markUnavailable("HAIR_NOT_DETECTED", "머리를 찾지 못했습니다", NOW);

        assertThatThrownBy(() -> unavailable.retry(NOW))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.JOB_RETRY_NOT_ALLOWED);
        assertThatThrownBy(() -> pending().retry(NOW))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.JOB_RETRY_NOT_ALLOWED);
    }

    /** 코드 없는 실패는 앱이 안내할 말이 없다. */
    @Test
    void refusesToFailWithoutAReasonCode() {
        AnalysisJob processing = pending().start(NOW);

        assertThatThrownBy(() -> processing.fail(" ", "사유 없음", NOW))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.JOB_FAILURE_REASON_REQUIRED);
        assertThatThrownBy(() -> processing.markUnavailable(null, null, NOW))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.JOB_FAILURE_REASON_REQUIRED);
    }

    @Test
    void refusesProgressOutsideZeroToHundred() {
        AnalysisJob processing = pending().start(NOW);

        assertThatThrownBy(() -> processing.progressTo(101))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.JOB_PROGRESS_INVALID);
        assertThatThrownBy(() -> processing.progressTo(-1))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.JOB_PROGRESS_INVALID);
    }

    /** 끝난 작업의 진행률은 더 이상 움직이지 않는다. */
    @Test
    void refusesProgressOnAJobThatIsNotProcessing() {
        assertThatThrownBy(() -> pending().progressTo(50))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.JOB_TRANSITION_INVALID);
    }

    @Test
    void refusesAnAttemptCountBelowOne() {
        assertThatThrownBy(() -> AnalysisJob.reconstitute(
                UUID.randomUUID(), USER_ID, RECORD_ID, PHOTO_ID, AnalysisJobStatus.PENDING,
                0, 0, null, null, null, null, NOW))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.JOB_ATTEMPT_COUNT_INVALID);
    }

    /** 사진이 지워져도 끝난 분석의 이력은 남는다 — 무효화 사건이지 삭제 사건이 아니다. */
    @Test
    void keepsAFinishedJobWhoseSubjectPhotoIsGone() {
        AnalysisJob orphaned = AnalysisJob.reconstitute(
                UUID.randomUUID(), USER_ID, RECORD_ID, null, AnalysisJobStatus.STALE,
                100, 1, null, null, NOW, NOW, NOW);

        assertThat(orphaned.photoId()).isNull();
    }

    /** 아직 돌고 있는 분석은 대상 없이 성립하지 않는다. */
    @Test
    void refusesAnInProgressJobWithoutASubjectPhoto() {
        assertThatThrownBy(() -> AnalysisJob.reconstitute(
                UUID.randomUUID(), USER_ID, RECORD_ID, null, AnalysisJobStatus.PROCESSING,
                10, 1, null, null, NOW, null, NOW))
                .isInstanceOf(AnalysisException.class)
                .hasFieldOrPropertyWithValue("error", AnalysisError.JOB_PHOTO_REQUIRED);
    }

    private AnalysisJob pending() {
        return AnalysisJob.create(USER_ID, RECORD_ID, PHOTO_ID, NOW);
    }
}
