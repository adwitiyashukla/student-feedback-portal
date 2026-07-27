package com.adwitiya.feedbackportal.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executors;

/**
 * Runs {@code @Async} work on Java 21 virtual threads.
 *
 * <p>The two async paths here — calling the Python analytics service and
 * sending notification mail — are both I/O bound and both must stay off the
 * request thread. Virtual threads make a thread-per-task executor the correct
 * choice rather than a tuned pool.</p>
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean("applicationTaskExecutor")
    @Override
    public AsyncTaskExecutor getAsyncExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, java.lang.reflect.Method method, Object... params) {
                log.error("Unhandled exception in async method {}", method.getName(), ex);
            }
        };
    }
}
