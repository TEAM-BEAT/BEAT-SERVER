package com.beat.infra.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class TaskExecutorConfig {

	private final ThreadPoolProperties threadPoolProperties;

	public TaskExecutorConfig(ThreadPoolProperties threadPoolProperties) {
		this.threadPoolProperties = threadPoolProperties;
	}

	@Bean(name = "applicationTaskExecutor")
	public ThreadPoolTaskExecutor applicationTaskExecutor() {
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

	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(threadPoolProperties.getCoreSize());
		scheduler.setThreadNamePrefix(threadPoolProperties.getThreadNamePrefix() + "scheduler-");
		scheduler.initialize();
		return scheduler;
	}
}
