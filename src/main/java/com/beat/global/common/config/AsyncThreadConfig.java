package com.beat.global.common.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import com.beat.global.common.handler.GlobalAsyncExceptionHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncThreadConfig implements AsyncConfigurer {
	private final ThreadPoolProperties threadPoolProperties;

	/**
	 * Creates and returns an Executor for asynchronous task execution.
	 *
	 * <p>The executor is configured with a core pool size and thread name prefix sourced from
	 * the thread pool properties. It employs a CallerRunsPolicy to handle task rejections,
	 * ensuring that when the pool is saturated, tasks execute in the calling thread.
	 * The ThreadPoolTaskExecutor is wrapped in a DelegatingSecurityContextExecutor to preserve
	 * the Spring Security context across asynchronous execution.
	 *
	 * @return the security-aware executor for asynchronous tasks
	 */
	@Override
	@Bean(name = "taskExecutor")
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(threadPoolProperties.getCoreSize());
		executor.setThreadNamePrefix(threadPoolProperties.getThreadNamePrefix());
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return new DelegatingSecurityContextExecutor(executor.getThreadPoolExecutor());
	}

	/**
	 * Returns the asynchronous uncaught exception handler.
	 *
	 * <p>This bean provides a global handler for uncaught exceptions thrown by asynchronous
	 * methods, using an instance of {@link GlobalAsyncExceptionHandler}.</p>
	 *
	 * @return an instance of {@link GlobalAsyncExceptionHandler} for handling uncaught async exceptions
	 */
	@Override
	@Bean
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return new GlobalAsyncExceptionHandler();
	}
}
