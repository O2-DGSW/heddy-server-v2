package com.heddy.domain.summary.port.out;

import com.heddy.domain.summary.model.MySummary;

import java.time.Instant;
import java.util.UUID;

public interface MySummaryQueryPort {

    /**
     * 카운트 4종을 한 번에 센다.
     *
     * @param now 공유의 만료 판정 기준 시각. 만료는 상태가 아니라 시각 비교라 호출부가 넘긴다.
     */
    MySummary count(UUID userId, Instant now);
}
