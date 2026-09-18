package com.heddy.adapter.out.ai;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class HairAnalysisAsyncConfigTest {

    @Test
    void appliesBackpressureOnTheCallingThreadWhenTheQueueIsFull() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor)
                new HairAnalysisAsyncConfig().hairAnalysisTaskExecutor(1);
        try {
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
        } finally {
            executor.shutdown();
        }
    }
}
