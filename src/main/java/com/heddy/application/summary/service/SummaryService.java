package com.heddy.application.summary.service;

import com.heddy.domain.summary.model.MySummary;
import com.heddy.domain.summary.port.in.GetMySummaryUseCase;
import com.heddy.domain.summary.port.out.MySummaryQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * 홈 요약 카운트. 집계 규칙을 서버 한 곳에 둬 화면마다 정의가 갈리는 것을 막는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SummaryService implements GetMySummaryUseCase {

    private final MySummaryQueryPort mySummaryQueryPort;

    @Override
    public MySummary get(UUID requesterId) {
        return mySummaryQueryPort.count(requesterId, Instant.now());
    }
}
