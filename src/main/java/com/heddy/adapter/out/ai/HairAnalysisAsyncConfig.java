package com.heddy.adapter.out.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
class HairAnalysisAsyncConfig {

    @Bean(name = "hairAnalysisTaskExecutor")
    Executor hairAnalysisTaskExecutor(
            @Value("${app.ai.worker-threads}") int workerThreads
    ) {
        if (workerThreads < 1 || workerThreads > 8) {
            throw new IllegalArgumentException("분석 워커 수는 1~8 이어야 합니다");
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workerThreads);
        executor.setMaxPoolSize(workerThreads);
        executor.setQueueCapacity(100);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("hair-analysis-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
