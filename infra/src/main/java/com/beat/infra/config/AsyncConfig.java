package com.beat.infra.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import com.beat.infra.InfraBaseConfig;

@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class AsyncConfig implements AsyncConfigurer, InfraBaseConfig {

	private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

	private final ThreadPoolProperties threadPoolProperties;
	private final BeanFactory beanFactory;

	public AsyncConfig(ThreadPoolProperties threadPoolProperties, BeanFactory beanFactory) {
		this.threadPoolProperties = threadPoolProperties;
		this.beanFactory = beanFactory;
	}

	@Bean(name = "applicationTaskExecutor")
	public ThreadPoolTaskExecutor applicationTaskExecutor() {
		return createTaskExecutor();
	}

	@Override
	@Bean(name = "taskExecutor")
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor executor = beanFactory.getBean("applicationTaskExecutor", ThreadPoolTaskExecutor.class);
		return new DelegatingSecurityContextExecutor(executor);
	}

	@Override
	@Bean
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (ex, method, params) -> {
			log.error("Async exception in method {}", method.getName(), ex);
		};
	}

	@Bean
	public TaskScheduler taskScheduler() {
		return createTaskScheduler();
	}

	private ThreadPoolTaskExecutor createTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(threadPoolProperties.getCoreSize());
		executor.setMaxPoolSize(Math.max(threadPoolProperties.getMaxPoolSize(), threadPoolProperties.getCoreSize()));
		executor.setQueueCapacity(threadPoolProperties.getQueueCapacity());
		executor.setThreadNamePrefix(threadPoolProperties.getThreadNamePrefix());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}

	private ThreadPoolTaskScheduler createTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(threadPoolProperties.getCoreSize());
		scheduler.setThreadNamePrefix(threadPoolProperties.getThreadNamePrefix() + "scheduler-");
		scheduler.initialize();
		return scheduler;
	}
}
