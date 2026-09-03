package com.heddy.application.summary.service;

import com.heddy.domain.summary.model.MySummary;
import com.heddy.domain.summary.port.out.MySummaryQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock MySummaryQueryPort mySummaryQueryPort;

    @InjectMocks SummaryService service;

    /** 만료 판정 기준 시각은 질의가 아니라 호출부가 정한다. 조회 시점이 그대로 넘어가야 한다. */
    @Test
    void asksForTheCountsAtTheCurrentInstant() {
        MySummary expected = new MySummary(14, 4, 5, 5);
        given(mySummaryQueryPort.count(eq(USER_ID), any(Instant.class))).willReturn(expected);
        Instant before = Instant.now();

        MySummary result = service.get(USER_ID);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<Instant> now = ArgumentCaptor.forClass(Instant.class);
        then(mySummaryQueryPort).should().count(eq(USER_ID), now.capture());
        assertThat(now.getValue()).isBetween(before, Instant.now());
    }
}
