package com.beat.infra.config;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import com.beat.infra.InfraBaseConfig;

@Configuration(proxyBeanMethods = false)
@EnableAsync
@Import(TaskExecutorConfig.class)
public class AsyncConfig implements AsyncConfigurer, InfraBaseConfig {

	private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

	private final ThreadPoolTaskExecutor applicationTaskExecutor;

	public AsyncConfig(@Qualifier("applicationTaskExecutor") ThreadPoolTaskExecutor applicationTaskExecutor) {
		this.applicationTaskExecutor = applicationTaskExecutor;
	}

	@Override
	@Bean(name = "taskExecutor")
	public Executor getAsyncExecutor() {
		return new DelegatingSecurityContextExecutor(applicationTaskExecutor);
	}

	@Override
	@Bean
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (ex, method, params) ->
			log.error("Async exception in method {}", method.getName(), ex);
	}
}
