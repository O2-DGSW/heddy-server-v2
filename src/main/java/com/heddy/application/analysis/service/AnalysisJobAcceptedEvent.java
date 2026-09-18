package com.heddy.application.analysis.service;

import java.util.UUID;

/** 작업 행 커밋 뒤 로컬 모델 워커를 깨우는 내부 사건. */
public record AnalysisJobAcceptedEvent(UUID jobId, UUID userId) {
}
