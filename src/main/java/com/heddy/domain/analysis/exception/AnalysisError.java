package com.heddy.domain.analysis.exception;

public enum AnalysisError {
    JOB_TRANSITION_INVALID("ANALYSIS_JOB_TRANSITION_INVALID", "허용되지 않는 분석 작업 상태 전이입니다."),
    JOB_PROGRESS_INVALID("ANALYSIS_JOB_PROGRESS_INVALID", "분석 진행률은 0~100 이어야 합니다."),
    JOB_ATTEMPT_COUNT_INVALID("ANALYSIS_JOB_ATTEMPT_COUNT_INVALID", "분석 시도 횟수는 1 이상이어야 합니다."),
    JOB_FAILURE_REASON_REQUIRED("ANALYSIS_JOB_FAILURE_REASON_REQUIRED", "실패·재촬영 상태에는 사유 코드가 필요합니다."),
    JOB_RETRY_NOT_ALLOWED("ANALYSIS_JOB_RETRY_NOT_ALLOWED", "실패한 분석만 재시도할 수 있습니다."),
    JOB_PHOTO_REQUIRED("ANALYSIS_JOB_PHOTO_REQUIRED", "진행 중인 분석에는 대상 사진이 필요합니다."),
    RESULT_SCORE_INVALID("ANALYSIS_RESULT_SCORE_INVALID", "분석 점수는 0~100 이어야 합니다."),
    RESULT_METRICS_INCOMPLETE("ANALYSIS_RESULT_METRICS_INCOMPLETE", "분석 지표 4종이 모두 필요합니다."),
    RESULT_MODEL_VERSION_REQUIRED("ANALYSIS_RESULT_MODEL_VERSION_REQUIRED", "분석 모델 버전은 필수입니다."),
    RESULT_GRADE_UNKNOWN("ANALYSIS_RESULT_GRADE_UNKNOWN", "저장된 분석 신뢰도 등급 값을 읽을 수 없습니다."),
    JOB_STATUS_UNKNOWN("ANALYSIS_JOB_STATUS_UNKNOWN", "저장된 분석 작업 상태 값을 읽을 수 없습니다.");

    private final String code;
    private final String message;

    AnalysisError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
