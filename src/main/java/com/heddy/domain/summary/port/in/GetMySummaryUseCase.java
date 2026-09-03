package com.heddy.domain.summary.port.in;

import com.heddy.domain.summary.model.MySummary;

import java.util.UUID;

public interface GetMySummaryUseCase {

    /** 자료가 하나도 없으면 모두 0 이다. 빈 상태는 오류가 아니다. */
    MySummary get(UUID requesterId);
}
